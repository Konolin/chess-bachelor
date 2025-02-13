import json
import os

import mysql.connector
import numpy as np
import chess
import matplotlib.pyplot as plt
import pandas as pd
from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}


def fetch_data():
    db_connection = mysql.connector.connect(**DB_CONFIG)

    query = """
        SELECT * FROM chess_positions 
        WHERE best_move IS NOT NULL 
        AND black_score IS NOT NULL 
        ORDER BY RAND()
    """

    train_df = pd.read_sql(query, db_connection)
    db_connection.close()

    ratio = int(len(train_df) * 0.8)
    val_df = train_df[ratio:]
    train_df = train_df[:ratio]

    return train_df, val_df


def one_hot_encode_piece(piece):
    pieces = list('rnbqkpRNBQKP.')
    arr = np.zeros(len(pieces))
    piece_to_index = {p: i for i, p in enumerate(pieces)}
    index = piece_to_index[piece]
    arr[index] = 1
    return arr


def encode_fen_string(fen_str):
    def encode_board(board_str):
        # Remove all the spaces
        board_str = board_str.replace(' ', '')
        board_list = []
        for row in board_str.split('\n'):
            row_list = []
            for piece in row:
                row_list.append(one_hot_encode_piece(piece))
            board_list.append(row_list)
        return np.array(board_list)

    board = chess.Board(fen=fen_str)
    return encode_board(str(board))


def save(model, history, version, metadata):
    def plot_history(hist):
        plt.style.use('ggplot')
        plt.plot(hist['loss'], label='train loss')
        plt.plot(hist['val_loss'], label='val loss')
        plt.legend()
        plt.title('Loss During Training')
        plt.show()

    # Save the model
    model.save(f"models/model_v{version}")

    # Save the metadata
    with open(f"models/metadata_v{version}.json", "w") as f:
        json.dump(metadata, f)

    # Save the history
    with open(f"models/history_v{version}.json", "w") as f:
        json.dump(history, f)

    # Plot the history
    plot_history(history)
