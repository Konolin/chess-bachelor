package com.example.backend.models.pieces;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;

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
            final int directedOffset = offset * this.getAlliance().getDirection();
            int candidatePosition = this.getPosition() + directedOffset;

            // skip invalid moves
            if (isContinueCase(directedOffset, candidatePosition)) {
                continue;
            }

            // get candidate tile
            final Tile candidateTile = board.getTileAtCoordinate(candidatePosition);

            // attack move
            if (offset == 7 || offset == 9) {
                // normal attack move
                if (candidateTile.isOccupied() && candidateTile.getOccupyingPiece().getAlliance() != this.getAlliance()) {
                    if (this.getAlliance().isPromotionSquare(candidatePosition)) {
                        // promote and attack
                        for (char promotionChar : getPromotionChars()) {
                            legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.PROMOTION_ATTACK, String.valueOf(promotionChar)));
                        }
                    } else {
                        legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.ATTACK));
                    }
                    // en passant attack move
                } else if (candidateTile.isEmpty() && isEnPassantMove(board, candidateTile, offset)) {
                    legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.EN_PASSANT));
                }
                // normal 1 tile move
            } else if (offset == 8 && candidateTile.isEmpty()) {
                if (this.getAlliance().isPromotionSquare(candidatePosition)) {
                    // promote
                    for (char promotionChar : getPromotionChars()) {
                        legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.PROMOTION, String.valueOf(promotionChar)));
                    }
                } else {
                    legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.NORMAL));
                }
                // normal 2 tile move
            } else if (isFirstMove() && offset == 16 && candidateTile.isEmpty() &&
                    board.getTileAtCoordinate(candidatePosition - 8 * this.getAlliance().getDirection()).isEmpty()) {
                legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.DOUBLE_PAWN_ADVANCE));
            }
        }

        return legalMoves;
    }

    private boolean isEnPassantMove(final Board board, final Tile candidateTile, final int offset) {
        final int neighbourPosition = this.getPosition() + (offset == 7 ? -1 : 1) * this.getAlliance().getDirection();
        final Tile neighbourTile = board.getTileAtCoordinate(neighbourPosition);

        if (!neighbourTile.isOccupied()) {
            return false;
        }

        final Piece neighbourPiece = neighbourTile.getOccupyingPiece();
        return candidateTile.isEmpty() && neighbourPiece.getAlliance() != this.getAlliance() &&
                neighbourPiece.equals(board.getEnPassantPawn());
    }

    @Override
    public Pawn movePiece(final Alliance alliance, final int toTilePosition) {
        return new Pawn(toTilePosition, alliance, false);
    }

    private boolean isContinueCase(final int offset, final int candidatePosition) {
        return !ChessUtils.isValidPosition(candidatePosition) ||
                isFirstOrEighthColumnExclusion(this.getPosition(), offset) ||
                Math.abs(offset) == 16 && !this.isFirstMove();
    }

    private boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int offset) {
        return ChessUtils.FIRST_COLUMN[currentPosition] && (offset == 7 || offset == -9) ||
                ChessUtils.EIGHTH_COLUMN[currentPosition] && (offset == -7 || offset == 9);
    }

    private List<Character> getPromotionChars() {
        // Use lowercase for black and uppercase for white
        if (this.getAlliance().isBlack()) {
            return List.of('q', 'r', 'b', 'n');
        } else {
            return List.of('Q', 'R', 'B', 'N');
        }
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "P" : "p";
    }

    @Override
    public boolean isPawn() {
        return true;
    }
}
