package com.example.backend.models.pieces;

import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.services.GameService;
import com.example.backend.utils.ChessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    private final Logger logger = LoggerFactory.getLogger(Pawn.class);
    private static final int[] MOVE_OFFSETS = {7, 8, 9, 16};

    public Pawn(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.PAWN);
    }

    @Override
    public List<Move> generateLegalMovesList(final Board board) {
        logger.info("pawn" + board.toString());

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
                        for (PieceType promotableType : PieceType.PROMOTABLE_TYPES) {
                            legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.PROMOTION_ATTACK, promotableType));
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
                    for (PieceType promotableType : PieceType.PROMOTABLE_TYPES) {
                        legalMoves.add(new Move(this.getPosition(), candidatePosition, MoveType.PROMOTION_ATTACK, promotableType));
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

    @Override
    public long generateLegalMovesBitBoard(Board board) {
        long legalMovesBitboard = 0L;

        for (final int offset : MOVE_OFFSETS) {
            final int directedOffset = offset * this.getAlliance().getDirection();
            int candidatePosition = this.getPosition() + directedOffset;

            // S]skip invalid moves
            if (isContinueCase(directedOffset, candidatePosition)) {
                continue;
            }

            // get candidate tile
            final Tile candidateTile = board.getTileAtCoordinate(candidatePosition);

            // attack move
            if (offset == 7 || offset == 9) {
                // normal attack move
                if (candidateTile.isOccupied() && candidateTile.getOccupyingPiece().getAlliance() != this.getAlliance()) {
                    legalMovesBitboard |= 1L << candidatePosition;
                    // en passant attack move
                } else if (candidateTile.isEmpty() && isEnPassantMove(board, candidateTile, offset)) {
                    legalMovesBitboard |= 1L << candidatePosition;
                }
                // normal 1-tile move
            } else if (offset == 8 && candidateTile.isEmpty()) {
                legalMovesBitboard |= 1L << candidatePosition;
                // normal 2-tile move
            } else if (isFirstMove() && offset == 16 && candidateTile.isEmpty() &&
                    board.getTileAtCoordinate(candidatePosition - 8 * this.getAlliance().getDirection()).isEmpty()) {
                legalMovesBitboard |= 1L << candidatePosition;
            }
        }

        return legalMovesBitboard;
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
        return ChessUtils.isPositionInColumn(currentPosition, 1) && (offset == 7 || offset == -9) ||
                ChessUtils.isPositionInColumn(currentPosition, 8) && (offset == -7 || offset == 9);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "P" : "p";
    }
}
