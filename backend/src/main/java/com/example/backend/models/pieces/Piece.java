package com.example.backend.models.pieces;

import com.example.backend.models.Move;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Piece {
    private final List<Move> legalMoves;
    private final int position;
    private final Alliance alliance;

    protected Piece(final int position, final Alliance alliance) {
        this.position = position;
        this.alliance = alliance;
        this.legalMoves = this.generateLegalMoves();
    }

    abstract List<Move> generateLegalMoves();
}
