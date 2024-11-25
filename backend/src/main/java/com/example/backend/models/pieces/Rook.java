package com.example.backend.models.pieces;

import com.example.backend.utils.ChessUtils;
import com.example.backend.models.moves.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.MoveType;

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

        for (final int offset: MOVE_OFFSETS) {
            int currentIterationPosition = this.getPosition();
            while (ChessUtils.isValidPosition(currentIterationPosition)) {
                final int candidatePosition = currentIterationPosition + offset;

                // reached board limits
                if (!ChessUtils.isValidPosition(candidatePosition) ||
                        isFirstOrEighthColumnExclusion(currentIterationPosition, offset)) {
                    break;
                }

                final Tile candidateTile = board.getTileAtCoordinate(candidatePosition);
                if (candidateTile.isEmpty()) {
                    // make normal move
                    legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.NORMAL));
                } else if (candidateTile.isOccupied()) {
                    if (candidateTile.getOccupyingPiece().getAlliance() != this.getAlliance()) {
                        // make attack move
                        legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.ATTACK));
                    }
                    break;
                }

                currentIterationPosition = candidatePosition;
            }
        }
        return legalMoves;
    }

    @Override
    public Rook movePiece(final Alliance alliance, final int toTilePosition) {
        return new Rook(toTilePosition, alliance, false);
    }

    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int offset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && offset == -1 ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && offset == 1;
    }

    @Override
    public boolean isRook() {
        return true;
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
