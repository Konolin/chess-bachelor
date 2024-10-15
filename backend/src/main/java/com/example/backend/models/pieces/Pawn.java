package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    private static final int[] CANDIDATE_MOVE_OFFSETS = {8, 16, 7, 9};

    public Pawn(final int position, final Alliance alliance) {
        super(position, alliance);
    }

    public Pawn(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove);
    }

    @Override
    public List<Move> calculateLegalMoves(Board board) {
        final List<Move> legalMoves = new ArrayList<>();

        for (final int offset : CANDIDATE_MOVE_OFFSETS) {
            final int candidatePosition = this.getPosition() + (this.getAlliance().getDirection() * offset);

            // jump over invalid moves due to exiting board bounds
            if (!ChessUtils.isValidTileCoordinate(candidatePosition)) {
                continue;
            }

            // normal move
            if (offset == 8 && !board.getTileAtPosition(candidatePosition).isTileOccupied()) {
                if (this.getAlliance().isPawnPromotionRow(candidatePosition)) {
                    // TODO - promotion
                } else {
                    // TODO - normal move
                }
            }
        }

        return legalMoves;
    }

    @Override
    public Pawn movePiece(final Move move) {
        return new Pawn(move.getDestinationPosition(), move.getPiece().getAlliance());
    }

    @Override
    public String toString() {
        return this.getAlliance().isBlack() ? "p" : "P";
    }
}
