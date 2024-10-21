package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    private static final int[] MOVE_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    public King(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        List<Move> legalMoves = new ArrayList<>();
        for (final int offset : MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset;
            if (!ChessUtils.isValidPosition(candidatePosition) || isFirstOrEighthColumnExclusion(this.getPosition(), offset)) {
                continue;
            }
            if (board.getTileAtCoordinate(candidatePosition).isEmpty()){
                legalMoves.add(new Move(this.getPosition(), candidatePosition));
            } else {
                if (board.getTileAtCoordinate(candidatePosition).getOccupyingPiece().getAlliance() != this.getAlliance()) {
                    legalMoves.add(new Move(this.getPosition(), candidatePosition));
                }
            }
        }
        return legalMoves;
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
}
