import chess
import chess.pgn
import mysql.connector
import os
import random
import re

from dotenv import load_dotenv

"""
    This script extracts FEN positions and evaluations from PGN files
    and inserts them into a MySQL database.
"""

# Load environment variables from .env file
load_dotenv()

# MySQL Connection Configuration
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}

PGN_DIRECTORY = "../data/pgn"
RANDOM_POSITIONS_PER_GAME = 50
BATCH_SIZE = 500

# Regular expression to extract evaluation from PGN comments.
# It captures evaluations like "0.19", "-1.5", or mate scores like "#7".
EVAL_REGEX = re.compile(r"\[%eval\s+([#\d\.\-]+)\]")


def normalize_evaluation(eval_str):
    """
    Converts an evaluation string into a normalized float in [-1, 1].
    For regular numeric evaluations (in pawn units), the value is clamped
    to [-max_eval, max_eval] (here max_eval=10) and then normalized.
    For mate evaluations (e.g. "#7" or "#-7"), it returns 1.0 or -1.0 respectively.
    """
    max_eval = 10.0  # maximum evaluation (in pawn units) we expect
    if eval_str.startswith('#'):
        # Mate evaluation: if mate is reported, we assign the extreme value.
        try:
            mate_moves = int(eval_str[1:])
            # Positive mate value means mate for White, negative means mate for Black.
            return 1.0 if mate_moves > 0 else -1.0
        except ValueError:
            return None
    else:
        # Numeric evaluation: try to convert to float and normalize
        try:
            value = float(eval_str)
        except ValueError:
            return None
        # Clamp the value to [-max_eval, max_eval] to avoid extreme outliers.
        clamped_value = max(-max_eval, min(max_eval, value))
        # Normalize to [-1, 1]
        normalized_value = clamped_value / max_eval
        return normalized_value


def insert_position_list_into_db(position_list):
    """
    Inserts extracted FEN positions and normalized evaluations into MySQL in batches.
    Each entry in position_list should be a tuple (fen, evaluation).
    The query uses INSERT IGNORE to avoid inserting duplicate positions.
    """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        query = """
            INSERT IGNORE INTO chess_positions_lichess 
            (fen, evaluation) VALUES (%s, %s)
        """

        # Insert positions in batches
        for i in range(0, len(position_list), BATCH_SIZE):
            batch = position_list[i:i + BATCH_SIZE]
            cursor.executemany(query, batch)
            conn.commit()
            print(f"Inserted {cursor.rowcount} new positions.")
    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")
    finally:
        cursor.close()
        conn.close()


def extract_positions_and_evaluations_from_pgn_file(filepath):
    """
    Extracts FEN positions and normalized evaluations from a PGN file in batches.
    Only positions with an evaluation in the PGN comment are processed.
    """
    position_list = []

    with open(filepath, "r", encoding="utf-8") as pgn_file:
        while True:
            try:
                game = chess.pgn.read_game(pgn_file)

                # Stop if no more games in the file.
                if game is None:
                    break

                    # Filter games based on player ratings.
                white_elo = game.headers.get("WhiteElo")
                black_elo = game.headers.get("BlackElo")
                if white_elo is None or black_elo is None:
                    continue
                if int(white_elo) < 2000 or int(black_elo) < 2000:
                    continue

                # Convert the game into a list of nodes (positions).
                nodes = list(game.mainline())
                if len(nodes) < 2:
                    continue  # Skip games without moves

                # Filter nodes (skipping the starting position) that contain an evaluation.
                nodes_with_eval = [node for node in nodes[1:] if EVAL_REGEX.search(node.comment)]
                # Skip games without any evaluation information.
                if not nodes_with_eval:
                    continue

                # Sample up to RANDOM_POSITIONS_PER_GAME nodes with eval data.
                sampled_nodes = random.sample(
                    nodes_with_eval,
                    min(RANDOM_POSITIONS_PER_GAME, len(nodes_with_eval))
                )

                # Get FEN positions and evaluations from sampled nodes.
                for node in sampled_nodes:
                    match = EVAL_REGEX.search(node.comment)
                    if match:
                        eval_str = match.group(1)
                        normalized_eval = normalize_evaluation(eval_str)
                        # Skip if conversion failed.
                        if normalized_eval is None:
                            continue
                        fen = node.board().fen()
                        position_list.append((fen, normalized_eval))

                        if len(position_list) >= BATCH_SIZE:
                            insert_position_list_into_db(position_list)
                            position_list.clear()

            except Exception as e:
                print(f"Error processing {filepath}: {e}")

    if position_list:
        insert_position_list_into_db(position_list)


def main():
    """
    Main function to extract positions and evaluations from all PGN files in the specified directory
    and insert them into MySQL in batches.
    """
    # Get a list of all PGN files in the directory
    pgn_files = [
        os.path.join(PGN_DIRECTORY, f)
        for f in os.listdir(PGN_DIRECTORY) if f.endswith(".pgn")
    ]

    # Process each PGN file
    for filepath in pgn_files:
        print(f"Processing {filepath}...")
        extract_positions_and_evaluations_from_pgn_file(filepath)


if __name__ == "__main__":
    main()
