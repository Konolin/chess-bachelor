package com.example.backend.models.pieces;

import com.example.backend.models.moves.Move;
import com.example.backend.models.board.Board;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Piece {
    private final int position;
    private final Alliance alliance;
    private final boolean isFirstMove;
    private final int cachedHashCode;

    protected Piece(final int position, final Alliance alliance, final boolean isFirstMove) {
        this.position = position;
        this.alliance = alliance;
        this.isFirstMove = isFirstMove;
        this.cachedHashCode = computeHashCode();
    }

    public abstract List<Move> generateLegalMoves(final Board board);

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Piece otherPiece)) {
            return false;
        }

        return position == otherPiece.getPosition() &&
                alliance == otherPiece.getAlliance() &&
                isFirstMove == otherPiece.isFirstMove();
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

    private int computeHashCode() {
        final int prime = 31;
        int result = alliance.hashCode();
        result = prime * result + position;
        result = prime * result + (isFirstMove ? 1 : 0);
        return result;
    }

    public abstract Piece movePiece(final Alliance alliance, final int toTilePosition);
}
