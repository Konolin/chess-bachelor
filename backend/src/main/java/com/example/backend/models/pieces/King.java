package com.example.backend.models.pieces;

import com.example.backend.utils.ChessUtils;
import com.example.backend.models.board.Board;

public class King extends Piece {
    private static final int[] MOVE_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    public King(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.KING);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long legalMovesBitboard = 0L;
        for (final int offset : MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset;
            if (!ChessUtils.isValidPosition(candidatePosition) || isFirstOrEighthColumnExclusion(this.getPosition(), offset)) {
                continue;
            }
            if (board.getTileAtCoordinate(candidatePosition).isEmpty()) {
                legalMovesBitboard |= (1L << candidatePosition);
            } else {
                if (board.getAllianceOfPieceAtPosition(candidatePosition) != this.getAlliance()) {
                    legalMovesBitboard |= (1L << candidatePosition);
                }
            }
        }
        return legalMovesBitboard;
    }


    @Override
    public King movePiece(final Alliance alliance, final int toTilePosition) {
        return new King(toTilePosition, alliance, false);
    }

    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -9 || candidateOffset == -1 || candidateOffset == 7) ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && (candidateOffset == -7 || candidateOffset == 1 || candidateOffset == 9);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "K" : "k";
    }

    @Override
    public boolean isKing() {
        return true;
    }
}
