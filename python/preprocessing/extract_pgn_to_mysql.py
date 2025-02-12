import chess
import chess.pgn
import mysql.connector
import os
import random

# MySQL Connection Configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "password",
    "database": "chess_db",
}

PGN_DIRECTORY = "../data/pgn"
RANDOM_POSITIONS_PER_GAME = 40


def insert_fen_into_db(fen_list):
    """ Inserts extracted FEN positions into MySQL """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # Insert FEN positions while avoiding duplicates
        query = "INSERT IGNORE INTO chess_positions (fen) VALUES (%s)"
        cursor.executemany(query, [(fen,) for fen in fen_list])

        conn.commit()
        print(f"Inserted {cursor.rowcount} new positions into database.")
    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")
    finally:
        cursor.close()
        conn.close()


def process_pgn_file(filepath):
    """
    Extracts FEN positions from a PGN file.
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

                # Randomly select positions
                sampled_indices = random.sample(range(len(move_list)), min(RANDOM_POSITIONS_PER_GAME, len(move_list)))
                for i in sampled_indices:
                    board_copy = board.copy()
                    for move in move_list[:i]:
                        board_copy.push(move)
                    fen_positions.append(board_copy.fen())

            except Exception as e:
                print(f"Error processing {filepath}: {e}")

    return fen_positions


def main():
    """
    Main function to extract positions from PGN files and insert them into MySQL.
    """
    pgn_files = [os.path.join(PGN_DIRECTORY, f) for f in os.listdir(PGN_DIRECTORY) if f.endswith(".pgn")]

    all_fens = []
    for filepath in pgn_files:
        print(f"Processing {filepath}...")
        all_fens.extend(process_pgn_file(filepath))

    if all_fens:
        insert_fen_into_db(all_fens)
    else:
        print("No positions extracted.")


if __name__ == "__main__":
    main()
