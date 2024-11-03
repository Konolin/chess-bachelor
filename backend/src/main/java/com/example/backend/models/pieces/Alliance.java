package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;

public enum Alliance {
    BLACK {
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
            return ChessUtils.EIGHTH_ROW[position];
        }

        @Override
        public String toString() {
            return "black";
        }
    },
    WHITE {
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
            return ChessUtils.FIRST_ROW[position];
        }

        @Override
        public String toString() {
            return "white";
        }
    };

    public abstract int getDirection();

    public abstract int getOppositeDirection();

    public abstract Alliance getOpponent();

    public boolean isWhite() {
        return this.equals(WHITE);
    }

    public boolean isBlack() {
        return this.equals(BLACK);
    }

    public abstract boolean isPromotionSquare(final int position);
}
