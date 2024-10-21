package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {
    private static final int[] MOVE_OFFSETS = {-9, -8, -7, -1, 1, 7, 8, 9};

    public Queen(final int position, final Alliance alliance) {
        super(position, alliance, false);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        List<Move> legalMoves = new ArrayList<>();
        for (final int offset: MOVE_OFFSETS) {
            int candidatePosition = this.getPosition() + offset;
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
                candidatePosition += offset;
            }
        }
        return legalMoves;
    }

    @Override
    public Queen movePiece(final Alliance alliance, final int toTilePosition) {
        return new Queen(toTilePosition, alliance);
    }

    private boolean isValidCandidatePosition(final int candidatePosition, final int offset) {
        return ChessUtils.isValidPosition(candidatePosition) &&
                !isFirstOrEighthColumnExclusion(this.getPosition(), offset);
    }

    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int offset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (offset == -9 || offset == -1 || offset == 7) ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && (offset == -7 || offset == 1 || offset == 9);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "Q" : "q";
    }
}
