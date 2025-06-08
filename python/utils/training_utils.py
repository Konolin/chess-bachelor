import json
import os
import numpy as np
import pandas as pd
import chess
import mysql.connector
from dotenv import load_dotenv
from matplotlib import pyplot as plt
import tensorflow as tf

"""
    This module contains utility functions to fetch data from a MySQL database, 
    encode FEN strings into NumPy arrays, create TFRecord files, and create a
    streaming tf.data.Dataset from TFRecord files.
"""

# Load environment variables from .env file
load_dotenv()

# Database configuration from environment variables
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}


def fetch_data_from_db():
    """
    Connects to the MySQL database and fetches chess positions with evaluations.
    Splits the resulting DataFrame into training (85%) and validation (15%) sets.

    Returns:
        (train_df, val_df): Two DataFrames with columns 'fen' and 'evaluation'.
    """
    db_connection = mysql.connector.connect(**DB_CONFIG)
    query = """
        SELECT * FROM chess_positions_lichess 
        WHERE evaluation IS NOT NULL 
    """
    df = pd.read_sql(query, db_connection)
    db_connection.close()

    # Split the data (85% training, 15% validation)
    ratio = int(len(df) * 0.85)
    train_df = df.iloc[:ratio]
    val_df = df.iloc[ratio:]
    return train_df, val_df


def encode_extra_features(fen_str):
    """
    Extract extra features from a FEN string. This includes side to move, castling rights, and en passant flag.
    Returns:
      [side_to_move, white kingside, white queenside, black kingside, black queenside, en passant flag]
    """
    board = chess.Board(fen=fen_str)
    side_to_move = 1 if board.turn == chess.BLACK else 0
    castling_rights = [
        1 if board.has_kingside_castling_rights(chess.WHITE) else 0,
        1 if board.has_queenside_castling_rights(chess.WHITE) else 0,
        1 if board.has_kingside_castling_rights(chess.BLACK) else 0,
        1 if board.has_queenside_castling_rights(chess.BLACK) else 0,
    ]
    en_passant_flag = 1 if board.ep_square is not None else 0
    return np.array([side_to_move] + castling_rights + [en_passant_flag], dtype=np.float32)


def encode_fen_to_bitboard(fen_str):
    """
        Encodes a FEN string into a 8×8×12 bitboard representation.
    """
    board = chess.Board(fen_str)

    bitboards = np.zeros((12, 64), dtype=np.float32)

    piece_map = board.piece_map()

    piece_to_index = {
        chess.PAWN: 0, chess.KNIGHT: 1, chess.BISHOP: 2,
        chess.ROOK: 3, chess.QUEEN: 4, chess.KING: 5
    }

    for square, piece in piece_map.items():
        idx = piece_to_index[piece.piece_type] + (0 if piece.color == chess.WHITE else 6)
        bitboards[idx][square] = 1

    bitboards = bitboards.reshape((8, 8, 12))
    return bitboards


def encode_fen_string(fen_str):
    """
    Encodes a FEN string into an 8×8×13 one-hot representation.
    """

    def one_hot_encode_piece(piece):
        pieces = list('rnbqkpRNBQKP.')
        arr = np.zeros(len(pieces), dtype=np.float32)
        piece_to_index = {p: i for i, p in enumerate(pieces)}
        index = piece_to_index[piece]
        arr[index] = 1.0
        return arr

    def encode_board(board_str):
        # Remove spaces and split into rows.
        board_str = board_str.replace(' ', '')
        board_list = []
        for row in board_str.split('\n'):
            # Ignore empty lines (if any)
            if row.strip() == "":
                continue
            row_list = [one_hot_encode_piece(piece) for piece in row]
            board_list.append(row_list)
        return np.array(board_list, dtype=np.float32)

    board = chess.Board(fen=fen_str)
    return encode_board(str(board))


def _float_feature(value):
    """
    Returns a float_list from a float / double.
    """
    return tf.train.Feature(float_list=tf.train.FloatList(value=value))


def create_tfrecord_file(df, filename):
    """
    Given a DataFrame with columns 'fen' and 'evaluation', encodes each row and writes a TFRecord file.
    Each TFRecord contains:
      - 'board': flattened 8×8×13 float32 values.
      - 'extra': 6 float32 values.
      - 'label': 1 float32 value (evaluation).
    """
    with tf.io.TFRecordWriter(filename) as writer:
        for idx, row in df.iterrows():
            fen = row['fen']
            label = float(row['evaluation'])
            board_encoded = encode_fen_to_bitboard(fen)
            board_flat = board_encoded.flatten().tolist()
            extra_features = encode_extra_features(fen).tolist()

            feature = {
                'board': _float_feature(board_flat),
                'extra': _float_feature(extra_features),
                'label': _float_feature([label])
            }
            example = tf.train.Example(features=tf.train.Features(feature=feature))
            writer.write(example.SerializeToString())
    print(f"✅ TFRecord file '{filename}' created with {len(df)} samples.")


def _parse_function(example_proto):
    """
    Parses a single record from a TFRecord file.
    Returns a tuple of features and label.
    """
    feature_description = {
        'board': tf.io.FixedLenFeature([8 * 8 * 12], tf.float32),
        'extra': tf.io.FixedLenFeature([6], tf.float32),
        'label': tf.io.FixedLenFeature([1], tf.float32),
    }
    parsed_features = tf.io.parse_single_example(example_proto, feature_description)
    board = tf.reshape(parsed_features['board'], (8, 8, 12))
    extra = parsed_features['extra']
    label = tf.squeeze(parsed_features['label'])  # Convert [label] to scalar
    return {'board_input': board, 'extra_input': extra}, label


def create_dataset(tfrecord_filename, batch_size):
    """
    Creates a streaming tf.data.Dataset from a TFRecord file.
    Returns a Dataset object with batches of size `batch_size`.
    """
    dataset = tf.data.TFRecordDataset(tfrecord_filename)
    dataset = dataset.map(_parse_function, num_parallel_calls=tf.data.AUTOTUNE)
    dataset = dataset.batch(batch_size)
    dataset = dataset.prefetch(tf.data.AUTOTUNE)
    return dataset


def save_model_and_history(model, history, version):
    """
    Saves the trained model and the training history (converted to native floats for JSON compatibility).
    Also plots and saves the loss history.
    """
    # Save model
    model.save(f"v{version}_model.keras")
    print(f"Model saved as v{version}_model.keras")

    # Convert history values to native Python floats
    history_converted = {key: [float(val) for val in values] for key, values in history.history.items()}

    # Save history as JSON
    with open(f"v{version}_history.json", "w") as f:
        json.dump(history_converted, f)
    print(f"Training history saved as v{version}_history.json")

    # Plot and save the loss curve
    plt.style.use('ggplot')
    plt.plot(history_converted['loss'], label='Train Loss')
    plt.plot(history_converted['val_loss'], label='Validation Loss')
    plt.title('Loss During Training')
    plt.xlabel('Epoch')
    plt.ylabel('Loss')
    plt.legend()
    plt.savefig(f"v{version}_loss_plot.png")
    plt.close()
    print(f"Loss plot saved as v{version}_loss_plot.png")
