import json
import os
import pickle

import mysql.connector
import numpy as np
import chess
import matplotlib.pyplot as plt
import pandas as pd
import joblib
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

    def one_hot_encode_piece(piece):
        pieces = list('rnbqkpRNBQKP.')
        arr = np.zeros(len(pieces))
        piece_to_index = {p: i for i, p in enumerate(pieces)}
        index = piece_to_index[piece]
        arr[index] = 1
        return arr

    board = chess.Board(fen=fen_str)
    return encode_board(str(board))


import pickle
from sklearn.preprocessing import StandardScaler

def load_scaler(filename="scaler.pkl"):
    """Loads a saved StandardScaler from a file."""
    try:
        with open(filename, "rb") as f:
            scaler = pickle.load(f)
            if not isinstance(scaler, StandardScaler):
                raise TypeError("Loaded object is not a StandardScaler instance")
            return scaler
    except Exception as e:
        print(f"Error loading scaler: {e}")
        return None  # Handle missing or invalid scaler gracefully



def save(model, history, version, metadata, scaler=None):
    def plot_history(hist):
        plt.style.use('ggplot')
        plt.plot(hist['loss'], label='train loss')
        plt.plot(hist['val_loss'], label='val loss')
        plt.legend()
        plt.title('Loss During Training')
        plt.savefig(f"v{version}_loss_plot.png")
        plt.show()

    # Save the model
    model.save(f"v{version}_model.keras")

    if scaler:
        with open(f"v{version}_scaler.pkl", "wb") as f:
            pickle.dump(scaler, f)

    # Save the metadata
    with open(f"v{version}_metadata.json", "w") as f:
        json.dump(metadata, f)

    # Save the history
    with open(f"v{version}_history.json", "w") as f:
        json.dump(history, f)

    # Plot the history
    plot_history(history)
