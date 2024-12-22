package com.example.backend.models.pieces;

import com.example.backend.utils.ChessUtils;

/**
 * The Alliance enum represents the two alliances in a chess game: WHITE and BLACK.
 * Each alliance has different behaviors and characteristics, such as the direction of movement,
 * the promotion squares for pawns, and the opponent's alliance.
 */
public enum Alliance {
    /**
     * The BLACK alliance.
     * Black pieces move in a positive direction and promote on the 8th rank.
     */
    BLACK {
        @Override
        public int getDirection() {
            return 1;
        }

        @Override
        public int getOppositeDirection() {
            return -1;
        }

        @Override
        public Alliance getOpponent() {
            return WHITE;
        }

        @Override
        public boolean isPromotionSquare(final int position) {
            return ChessUtils.isPositionInRow(position, 8);
        }

        @Override
        public String toString() {
            return "black";
        }
    },

    /**
     * The WHITE alliance.
     * White pieces move in a negative direction and promote on the 1st rank.
     */
    WHITE {
        @Override
        public int getDirection() {
            return -1;
        }

        @Override
        public int getOppositeDirection() {
            return 1;
        }


        @Override
        public Alliance getOpponent() {
            return BLACK;
        }

        @Override
        public boolean isPromotionSquare(final int position) {
            return ChessUtils.isPositionInRow(position, 1);
        }

        @Override
        public String toString() {
            return "white";
        }
    };

    /**
     * Gets the direction of movement for the pieces in the alliance.
     * For white, this is upwards on the board (negative direction), and for black, it is downwards (positive direction).
     *
     * @return The direction of movement for pieces in the alliance.
     */
    public abstract int getDirection();

    /**
     * Gets the opposite direction of movement for the pieces in the alliance.
     * For white, this is downwards on the board (positive direction), and for black, it is upwards (negative direction).
     *
     * @return The opposite direction of movement for pieces in the alliance.
     */
    public abstract int getOppositeDirection();

    /**
     * Gets the opponent alliance for the current alliance.
     * If the current alliance is white, the opponent is black, and vice versa.
     *
     * @return The opponent alliance.
     */
    public abstract Alliance getOpponent();

    /**
     * Checks if the current alliance is white.
     *
     * @return True if the current alliance is white, false otherwise.
     */
    public boolean isWhite() {
        return this.equals(WHITE);
    }

    /**
     * Checks if the current alliance is black.
     *
     * @return True if the current alliance is black, false otherwise.
     */
    public boolean isBlack() {
        return this.equals(BLACK);
    }

    /**
     * Checks if the given position corresponds to a promotion square for the current alliance's pawns.
     *
     * @param position The position to check.
     * @return True if the position is a promotion square for the current alliance's pawns, false otherwise.
     */
    public abstract boolean isPromotionSquare(final int position);
}
