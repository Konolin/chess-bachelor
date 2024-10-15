package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;

public class Queen extends Piece {
    public Queen(final int position, final Alliance alliance) {
        super(position, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isBlack() ? "q" : "Q";
    }
}
