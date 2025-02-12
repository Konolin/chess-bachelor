import os
import chess
import chess.engine
import mysql.connector
import multiprocessing
from dotenv import load_dotenv

load_dotenv()

# ----------------------
# Configuration
# ----------------------
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "database": os.getenv("DB_NAME"),
}

STOCKFISH_PATH = "../engines/stockfish.exe"
EVALUATION_SEARCH_DEPTH = 15
SAFE_CORES = max(1, int(multiprocessing.cpu_count() * 0.8))
BATCH_SIZE = 1000  # Number of positions to evaluate in each batch


def get_db_connection():
    """
    Create and return a new MySQL database connection using the global DB_CONFIG.
    """
    return mysql.connector.connect(**DB_CONFIG)


def fetch_unrated_positions(conn, limit=100):
    """
    Fetch up to `limit` FEN positions that need evaluation.
    Returns a list of (id, fen).
    """
    try:
        with conn.cursor() as cursor:
            query = """
                SELECT id, fen
                FROM chess_positions
                WHERE black_score IS NULL OR best_move IS NULL
                LIMIT %s
            """
            cursor.execute(query, (limit,))
            positions = cursor.fetchall()
        return positions
    except mysql.connector.Error as err:
        print(f"MySQL Error during fetch_unrated_positions: {err}")
        return []


def evaluate_position(position_data):
    """
    Evaluates a given FEN position using Stockfish from Black's perspective.
    Returns (position_id, black_score, best_move_uci).
    """
    position_id, board_fen = position_data

    try:
        # Start up a new Stockfish engine instance for each process
        with chess.engine.SimpleEngine.popen_uci(STOCKFISH_PATH) as engine:
            # Optionally configure engine to use a single thread
            engine.configure({"Threads": 1})

            board = chess.Board(board_fen)
            info = engine.analyse(board, chess.engine.Limit(depth=EVALUATION_SEARCH_DEPTH))

            # Initialize defaults
            score = 0
            best_move = None

            # Check if there's a score in the engine output
            if "score" in info and info["score"]:
                # Get the score from Black's perspective
                score = info["score"].pov(chess.BLACK).score(mate_score=10000)
                best_move = info.get("pv", [None])[0]

            return position_id, score, best_move.uci() if best_move else None

    except Exception as e:
        print(f"Error evaluating FEN {board_fen}: {e}")
        return position_id, None, None


def update_positions_in_db(conn, evaluated_data):
    """
    Updates the evaluated data in the database.
    Only updates rows where 'score' is not None.
    """
    try:
        with conn.cursor() as cursor:
            query = """
                UPDATE chess_positions
                SET black_score = %s, best_move = %s
                WHERE id = %s
            """
            # Filter out any (pos_id, None, None) results
            update_rows = [(score, best_move, pos_id)
                           for (pos_id, score, best_move) in evaluated_data
                           if score is not None]

            cursor.executemany(query, update_rows)
            conn.commit()
            print(f"Updated {cursor.rowcount} positions in database.")
    except mysql.connector.Error as err:
        print(f"MySQL Error during update_positions_in_db: {err}")


def reset_evaluations():
    """ Resets all evaluations in the database (sets black_score and best_move to NULL). """
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            query = """
                UPDATE chess_positions
                SET black_score = NULL, best_move = NULL
            """
            cursor.execute(query)
            conn.commit()
            print(f"Reset {cursor.rowcount} positions in database.")
    except mysql.connector.Error as err:
        print(f"MySQL Error during reset_evaluations: {err}")
    finally:
        conn.close()


def main():
    """
    Main function to evaluate FEN positions in steps using multiprocessing.
    Fetches positions in batches of BATCH_SIZE, evaluates them with parallel processes,
    then updates the database. Loops until there are no unrated positions left.
    """
    while True:
        # Open a connection for this batch
        conn = get_db_connection()

        # Fetch up to BATCH_SIZE positions to evaluate
        positions = fetch_unrated_positions(conn, limit=BATCH_SIZE)
        if not positions:
            print("No unrated positions left to evaluate.")
            conn.close()
            break

        print(f"Evaluating {len(positions)} positions using {SAFE_CORES} cores...")

        # Evaluate positions in parallel
        with multiprocessing.Pool(SAFE_CORES) as pool:
            evaluated_positions = pool.map(evaluate_position, positions)

        # Update the database with our evaluation results
        update_positions_in_db(conn, evaluated_positions)

        conn.close()
        print("Batch complete. Fetching next set...")

# ----------------------
# Script Entry
# ----------------------
if __name__ == "__main__":
    main()
