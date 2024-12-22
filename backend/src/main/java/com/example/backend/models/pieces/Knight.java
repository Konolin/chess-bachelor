package com.example.backend.models.pieces;

import com.example.backend.utils.ChessUtils;
import com.example.backend.models.board.Board;

public class Knight extends Piece {
    // TODO: use bitboardutils mask
    private static final int[] MOVE_OFFSETS = {-17, -15, -10, -6, 6, 10, 15, 17};

    public Knight(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.KNIGHT);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long legalMovesBitboard = 0L;
        for (final int offset : MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset;
            if (!ChessUtils.isValidPosition(candidatePosition) || isColumnExceptions(this.getPosition(), offset)) {
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
    public Knight movePiece(final Alliance alliance, final int toTilePosition) {
        return new Knight(toTilePosition, alliance);
    }

    private boolean isColumnExceptions(final int position, final int offset) {
        return isEighthColumnExclusion(position, offset) ||
                isSeventhColumnExclusion(position, offset) ||
                isSecondColumnExclusion(position, offset) ||
                isFirstColumnExclusion(position, offset);
    }

    private boolean isFirstColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -17 || candidateOffset == -10 ||
                candidateOffset == 6 || candidateOffset == 15);
    }

    private boolean isSecondColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.SECOND_COLUMN[currentPosition] && (candidateOffset == -10 || candidateOffset == 6);
    }

    private boolean isSeventhColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.SEVENTH_COLUMN[currentPosition] && (candidateOffset == -6 || candidateOffset == 10);
    }

    private boolean isEighthColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.EIGHTH_COLUMN[currentPosition] && (candidateOffset == -15 || candidateOffset == -6 ||
                candidateOffset == 10 || candidateOffset == 17);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "N" : "n";
    }
}
