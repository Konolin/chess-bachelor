package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;

public class Knight extends Piece {
    public Knight(final int position, final Alliance alliance) {
        super(position, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isBlack() ? "n" : "N";
    }
}
