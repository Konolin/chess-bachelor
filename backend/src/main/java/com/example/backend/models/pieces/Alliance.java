package com.example.backend.models.pieces;

public enum Alliance {
    BLACK {
        public int getDirection() {
            return 1;
        }
    },
    WHITE {
        public int getDirection() {
            return -1;
        }
    };

    public abstract int getDirection();

    public boolean isWhite() {
        return this.equals(BLACK);
    }

    public boolean isBlack() {
        return this.equals(WHITE);
    }
}
