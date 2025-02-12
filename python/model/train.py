import os

import mysql.connector
import pandas as pd
import numpy as np
import chess
import matplotlib.pyplot as plt
from dotenv import load_dotenv
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Flatten

load_dotenv()

DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}


def fetch_data():
    db_connection = mysql.connector.connect(**DB_CONFIG)

    query = ("SELECT * FROM chess_positions "
             "WHERE best_move IS NOT NULL "
             "AND black_score IS NOT NULL "
             "ORDER BY RAND()")

    train_df = pd.read_sql(query, db_connection)
    db_connection.close()
    # train_df = pd.read_csv('../data/train.csv', index_col='id')

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


def encode_board(board):
    # first lets turn the board into a string
    board_str = str(board)
    # then lets remove all the spaces
    board_str = board_str.replace(' ', '')
    board_list = []
    for row in board_str.split('\n'):
        row_list = []
        for piece in row:
            row_list.append(one_hot_encode_piece(piece))
        board_list.append(row_list)
    return np.array(board_list)


def encode_fen_string(fen_str):
    board = chess.Board(fen=fen_str)
    return encode_board(board)


def plot_history(history):
    plt.style.use('ggplot')
    plt.plot(history.history['loss'], label='train loss')
    plt.plot(history.history['val_loss'], label='val loss')
    plt.legend()
    plt.title('Loss During Training')
    plt.show()


def main():
    train_df, val_df = fetch_data()

    x_train = np.stack(train_df['fen'].apply(encode_fen_string))
    y_train = train_df['black_score']

    x_val = np.stack(val_df['fen'].apply(encode_fen_string))
    y_val = val_df['black_score']

    model = Sequential([
        Flatten(),
        Dense(1024, activation='relu'),
        Dense(64, activation='relu'),
        Dense(1),
    ])

    model.compile(
        optimizer='rmsprop',
        loss='mean_squared_error')

    history = model.fit(
        x_train,
        y_train,
        epochs=20,
        validation_data=(x_val, y_val))

    plot_history(history)


if __name__ == "__main__":
    main()
