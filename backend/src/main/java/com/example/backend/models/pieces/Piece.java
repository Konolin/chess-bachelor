package com.example.backend.models.pieces;

import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Piece {
    private final int position;
    private final Alliance alliance;
    private final boolean isFirstMove;

    protected Piece(final int position, final Alliance alliance, final boolean isFirstMove) {
        this.position = position;
        this.alliance = alliance;
        this.isFirstMove = isFirstMove;
    }

    public abstract List<Move> generateLegalMoves(final Board board);
}
