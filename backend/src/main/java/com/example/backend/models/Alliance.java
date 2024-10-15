package com.example.backend.models;

public enum Alliance {
    WHITE,
    BLACK;

    public boolean isWhite() {
        return this == WHITE;
    }

    public boolean isBlack() {
        return this == BLACK;
    }
}
