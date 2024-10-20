package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    private static final int[] MOVE_OFFSETS = {7, 8, 9, 16};

    public Pawn(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        List<Move> legalMoves = new ArrayList<>();

        for (final int offset : MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset * this.getAlliance().getDirection();

            if (!ChessUtils.isValidPosition(candidatePosition) || isFirstOrEighthColumnExclusion(this.getPosition(), offset)) {
                continue;
            }
            if (offset == 16 && !this.isFirstMove()) {
                continue;
            }

            Tile candidateTile = board.getTileAtCoordinate(candidatePosition);
            if ((offset == 7 || offset == 9) && candidateTile.isOccupied()) {
                if (candidateTile.getOccupyingPiece().getAlliance() != this.getAlliance()) {
                    legalMoves.add(new Move(this.getPosition(), candidatePosition));
                }
            } else if (offset == 8 && candidateTile.isEmpty()) {
                legalMoves.add(new Move(this.getPosition(), candidatePosition));
            } else if (offset == 16 && candidateTile.isEmpty() && board.getTileAtCoordinate(candidatePosition - 8).isEmpty()) {
                legalMoves.add(new Move(this.getPosition(), candidatePosition));
            }
        }

        return legalMoves;
    }


    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int candidateOffset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (candidateOffset == -9 || candidateOffset == -1 || candidateOffset == 7) ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && (candidateOffset == -7 || candidateOffset == 1 || candidateOffset == 9);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "P" : "p";
    }
}
