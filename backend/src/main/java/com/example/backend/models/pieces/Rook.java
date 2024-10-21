package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {
    private static final int[] MOVE_OFFSETS = {-8, -1, 1, 8};

    public Rook(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        List<Move> legalMoves = new ArrayList<>();
        for (final int offset : MOVE_OFFSETS) {
            final int candidatePosition = this.getPosition() + offset;
            while (isValidCandidatePosition(candidatePosition, offset)) {
                final Tile candidateTile = board.getTileAtCoordinate(candidatePosition);
                if (candidateTile.isEmpty()) {
                    legalMoves.add(new Move(this.getPosition(), candidatePosition));
                } else {
                    if (candidateTile.getOccupyingPiece().getAlliance() != this.getAlliance()) {
                        legalMoves.add(new Move(this.getPosition(), candidatePosition));
                    }
                    break;
                }
            }
        }
        return legalMoves;
    }

    private boolean isValidCandidatePosition(final int candidatePosition, final int offset) {
        return ChessUtils.isValidPosition(candidatePosition) &&
                !isFirstOrEighthColumnExclusion(this.getPosition(), offset);
    }

    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int offset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && offset == -1 ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && offset == 1;
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
