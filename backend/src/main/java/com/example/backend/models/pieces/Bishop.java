package com.example.backend.models.pieces;

import com.example.backend.models.Move;
import com.example.backend.models.board.Board;

import java.util.List;

public class Bishop extends Piece {
    public Bishop(final int position, final Alliance alliance) {
        super(position, alliance, false);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        return List.of();
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "B" : "b";
    }
}
