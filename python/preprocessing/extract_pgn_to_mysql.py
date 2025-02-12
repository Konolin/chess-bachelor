import chess
import chess.pgn
import mysql.connector
import os
import random

# MySQL Connection Configuration
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}

PGN_DIRECTORY = "../data/pgn"
RANDOM_POSITIONS_PER_GAME = 40
BATCH_SIZE = 500


def insert_fen_into_db(fen_list):
    """ Inserts extracted FEN positions into MySQL in batches """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        query = "INSERT IGNORE INTO chess_positions (fen) VALUES (%s)"

        for i in range(0, len(fen_list), BATCH_SIZE):
            batch = fen_list[i:i + BATCH_SIZE]
            cursor.executemany(query, [(fen,) for fen in batch])
            conn.commit()
            print(f"Inserted {cursor.rowcount} new positions.")
    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")
    finally:
        cursor.close()
        conn.close()


def process_pgn_file(filepath):
    """
    Extracts FEN positions from a PGN file in batches.
    """
    fen_positions = []

    with open(filepath, "r", encoding="utf-8") as pgn_file:
        while True:
            try:
                game = chess.pgn.read_game(pgn_file)
                if game is None:
                    break  # End of file

                board = game.board()
                move_list = list(game.mainline_moves())

                if len(move_list) < 10:
                    continue  # Skip short games

                sampled_indices = random.sample(range(len(move_list)), min(RANDOM_POSITIONS_PER_GAME, len(move_list)))
                for i in sampled_indices:
                    board_copy = board.copy()
                    for move in move_list[:i]:
                        board_copy.push(move)
                    fen_positions.append(board_copy.fen())

                    if len(fen_positions) >= BATCH_SIZE:
                        insert_fen_into_db(fen_positions)
                        fen_positions.clear()
            except Exception as e:
                print(f"Error processing {filepath}: {e}")

    if fen_positions:
        insert_fen_into_db(fen_positions)  # Insert any remaining positions


def main():
    """
    Main function to extract positions from PGN files and insert them into MySQL in batches.
    """
    pgn_files = [os.path.join(PGN_DIRECTORY, f) for f in os.listdir(PGN_DIRECTORY) if f.endswith(".pgn")]

    for filepath in pgn_files:
        print(f"Processing {filepath}...")
        process_pgn_file(filepath)


if __name__ == "__main__":
    main()
