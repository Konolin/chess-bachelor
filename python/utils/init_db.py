import os

import mysql.connector
from dotenv import load_dotenv

"""
    This script initializes the MySQL database for storing chess positions.
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


def initialize_database():
    """
    Creates the necessary table for storing chess positions.
    The table has the following columns:
    - id: Primary key (int, auto-increment)
    - fen: FEN representation of the chess position (varchar-100, unique)
    - evaluation: Evaluation of the chess position (float)
    """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS chess_positions_lichess (
                id INT AUTO_INCREMENT PRIMARY KEY,
                fen VARCHAR(100) UNIQUE NOT NULL,
                evaluation FLOAT
            )
        """)

        conn.commit()
        print("Database initialized successfully.")

    except mysql.connector.Error as err:
        print(f"MySQL Error: {err}")
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    initialize_database()