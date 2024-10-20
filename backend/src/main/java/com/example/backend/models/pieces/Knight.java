package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    private final static int[] MOVE_OFFSETS = {-17, -15, -10, -6, 6, 10, 15, 17};

    public Knight(int position, Alliance alliance) {
        super(position, alliance);
    }

    @Override
    public List<Move> generateLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();

        for (int offset : MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset;
            if (!ChessUtils.isValidPosition(candidatePosition) || isColumnExceptions(this.getPosition(), offset)) {
                continue;
            }

            legalMoves.add(new Move(this.getPosition(), candidatePosition));
        }

        return legalMoves;
    }

    private boolean isColumnExceptions(final int position, final int offset) {
        return isEighthColumnExclusion(position, offset) ||
                isSeventhColumnExclusion(position, offset) ||
                isSecondColumnExclusion(position, offset) ||
                isFirstColumnExclusion(position, offset);
    }

    private static boolean isFirstColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -17 || candidateOffset == -10 ||
                candidateOffset == 6 || candidateOffset == 15);
    }

    private static boolean isSecondColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.SECOND_COLUMN[currentPosition] && (candidateOffset == -10 || candidateOffset == 6);
    }

    private static boolean isSeventhColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.SEVENTH_COLUMN[currentPosition] && (candidateOffset == -6 || candidateOffset == 10);
    }

    private static boolean isEighthColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.EIGHTH_COLUMN[currentPosition] && (candidateOffset == -15 || candidateOffset == -6 ||
                candidateOffset == 10 || candidateOffset == 17);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "N" : "n";
    }
}
