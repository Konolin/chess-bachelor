package com.example.backend.models.pieces;

public enum Alliance {
    BLACK {
        public int getDirection() {
            return 1;
        }

        @Override
        public Alliance getOpponent() {
            return WHITE;
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
        public Alliance getOpponent() {
            return BLACK;
        }

        @Override
        public String toString() {
            return "white";
        }
    };

    public abstract int getDirection();

    public abstract Alliance getOpponent();

    public boolean isWhite() {
        return this.equals(BLACK);
    }

    public boolean isBlack() {
        return this.equals(WHITE);
    }
}
