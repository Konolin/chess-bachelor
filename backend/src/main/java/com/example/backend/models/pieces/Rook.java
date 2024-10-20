package com.example.backend.models.pieces;

import com.example.backend.models.Move;

import java.util.List;

public class Rook extends Piece {
    public Rook(int position, Alliance alliance) {
        super(position, alliance);
    }

    @Override
    List<Move> generateLegalMoves() {
        return List.of();
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
