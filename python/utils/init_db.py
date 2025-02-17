import mysql.connector

# MySQL Connection Configuration
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "password",
    "database": "chess_db",
}


def initialize_database():
    """ Creates the necessary tables for storing chess positions. """
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS chess_positions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                fen VARCHAR(100) UNIQUE NOT NULL,
                black_score INT,
                best_move VARCHAR(10)
            )
        """)

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