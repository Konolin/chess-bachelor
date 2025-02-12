import os

import chess
import chess.engine
import mysql.connector
import multiprocessing
import itertools

# MySQL Configuration
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}

STOCKFISH_PATH = "../engines/stockfish.exe"
EVALUATION_SEARCH_DEPTH = 15
SAFE_CORES = max(1, int(multiprocessing.cpu_count() * 0.5))
BATCH_SIZE = 1000 # Number of positions to evaluate in each batch


def fetch_unrated_positions(limit=100):
    """ Fetch a limited number of FEN positions that need evaluation. """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        query = """
        SELECT id, fen FROM chess_positions
        WHERE black_score IS NULL OR best_move IS NULL
        LIMIT %s
        """
        cursor.execute(query, (limit,))
        positions = cursor.fetchall()

        conn.close()
        return positions  # Returns a list of (id, fen)

    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")
        return []


def evaluate_position(position_data):
    """ Evaluates a given FEN position using Stockfish. """
    position_id, board_fen = position_data

    try:
        with chess.engine.SimpleEngine.popen_uci(STOCKFISH_PATH) as engine:
            board = chess.Board(board_fen)
            info = engine.analyse(board, chess.engine.Limit(depth=EVALUATION_SEARCH_DEPTH))

            if "score" in info:
                score = info["score"].relative.score(mate_score=10000) if info["score"].relative else 0
                best_move = info.get("pv", [None])[0]  # Get the best move if available
            else:
                score, best_move = 0, None

            return position_id, score, best_move.uci() if best_move else None

    except Exception as e:
        print(f"Error evaluating FEN {board_fen}: {e}")
        return position_id, None, None


def update_position_in_db(evaluated_data):
    """ Updates evaluated FEN positions in MySQL. """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        query = """
        UPDATE chess_positions
        SET black_score = %s, best_move = %s
        WHERE id = %s
        """
        cursor.executemany(query, [(score, best_move, pos_id) for pos_id, score, best_move in evaluated_data if
                                   score is not None])

        conn.commit()
        print(f"Updated {cursor.rowcount} positions in database.")

    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")

    finally:
        cursor.close()
        conn.close()


def main():
    """ Main function to evaluate FEN positions in steps using multiprocessing. """
    while True:
        positions = fetch_unrated_positions(limit=BATCH_SIZE)
        if not positions:
            print("No unrated positions left to evaluate.")
            break

        print(f"Evaluating {len(positions)} positions using {SAFE_CORES} cores...")

        with multiprocessing.Pool(SAFE_CORES) as pool:
            evaluated_positions = pool.map(evaluate_position, positions)

        update_position_in_db(evaluated_positions)

        print("Batch complete. Fetching next set...")


if __name__ == "__main__":
    main()
