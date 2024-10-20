package com.example.backend.models.pieces;

import com.example.backend.models.Move;

import java.util.List;

public class King extends Piece {
    public King(int position, Alliance alliance) {
        super(position, alliance);
    }

    @Override
    List<Move> generateLegalMoves() {
        return List.of();
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "K" : "k";
    }
}
