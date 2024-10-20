package com.example.backend.models.pieces;

public enum Alliance {
    BLACK,
    WHITE;

    public boolean isWhite() {
        return this.equals(BLACK);
    }

    public boolean isBlack() {
        return this.equals(WHITE);
    }
}
