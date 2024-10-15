package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;

public class King extends Piece {
    public King(final int position, final Alliance alliance) {
        super(position, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isBlack() ? "k" : "K";
    }
}
