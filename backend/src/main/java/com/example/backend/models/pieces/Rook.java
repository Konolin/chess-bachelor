package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;

public class Rook extends Piece {
    public Rook(final int position, final Alliance alliance) {
        super(position, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isBlack() ? "r" : "R";
    }
}
