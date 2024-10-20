package com.example.backend.models.pieces;

import com.example.backend.models.Move;
import com.example.backend.models.board.Board;

import java.util.List;

public class Rook extends Piece {
    public Rook(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        return List.of();
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
