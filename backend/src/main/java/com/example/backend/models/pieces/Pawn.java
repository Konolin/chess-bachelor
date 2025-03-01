package com.example.backend.models.pieces;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.utils.ChessUtils;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    private static final int[] MOVE_OFFSETS = {7, 8, 9, 16};

    public Pawn(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.PAWN);
    }

    public static List<Move> generateLegalMovesList(final Board board, final int piecePosition) {
        List<Move> legalMoves = new ArrayList<>();

        for (final int offset : MOVE_OFFSETS) {
            final int directedOffset = offset * board.getMoveMaker().getDirection();
            int candidatePosition = piecePosition + directedOffset;

            // skip invalid moves
            if (isContinueCase(directedOffset, candidatePosition, piecePosition, board.getMoveMaker())) {
                continue;
            }

            // attack move
            if (offset == 7 || offset == 9) {
                // normal attack move
                if (board.isTileOccupied(candidatePosition) && board.getAllianceOfTile(candidatePosition) != board.getMoveMaker()) {
                    if (board.getMoveMaker().isPromotionSquare(candidatePosition)) {
                        // promote and attack
                        for (PieceType promotableType : PieceType.PROMOTABLE_TYPES) {
                            legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.PROMOTION_ATTACK, promotableType));
                        }
                    } else {
                        legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.ATTACK));
                    }
                    // en passant attack move
                } else if (!board.isTileOccupied(candidatePosition) && isEnPassantMove(board, candidatePosition, offset, piecePosition)) {
                    legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.EN_PASSANT));
                }
                // normal 1 tile move
            } else if (offset == 8 && !board.isTileOccupied(candidatePosition)) {
                if (board.getMoveMaker().isPromotionSquare(candidatePosition)) {
                    // promote
                    for (PieceType promotableType : PieceType.PROMOTABLE_TYPES) {
                        legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.PROMOTION_ATTACK, promotableType));
                    }
                } else {
                    legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.NORMAL));
                }
                // normal 2 tile move
            } else if (isDoublePawnAdvanceable(piecePosition, board.getMoveMaker()) && offset == 16 && !board.isTileOccupied(candidatePosition) &&
                    !board.isTileOccupied(candidatePosition - 8 * board.getMoveMaker().getDirection())) {
                legalMoves.add(new Move(piecePosition, candidatePosition, MoveType.DOUBLE_PAWN_ADVANCE));
            }
        }

        return legalMoves;
    }

    public static long generateLegalMovesBitBoard(Board board, final int piecePosition) {
        long legalMovesBitboard = 0L;

        for (final int offset : MOVE_OFFSETS) {
            final int directedOffset = offset * board.getMoveMaker().getDirection();
            int candidatePosition = piecePosition + directedOffset;

            // skip invalid moves
            if (isContinueCase(directedOffset, candidatePosition, piecePosition, board.getMoveMaker())) {
                continue;
            }

            // attack move
            if (offset == 7 || offset == 9) {
                // normal attack move or en passant attack move
                if ((board.isTileOccupied(candidatePosition) && board.getAllianceOfTile(candidatePosition) != board.getMoveMaker()) ||
                        (!board.isTileOccupied(candidatePosition) && isEnPassantMove(board, candidatePosition, offset, piecePosition))) {
                    legalMovesBitboard |= 1L << candidatePosition;
                }
                // normal 1-tile move
            } else if (offset == 8 && !board.isTileOccupied(candidatePosition)) {
                legalMovesBitboard |= 1L << candidatePosition;
                // normal 2-tile move
            } else if (isDoublePawnAdvanceable(piecePosition, board.getMoveMaker()) && offset == 16 && !board.isTileOccupied(candidatePosition) &&
                    !board.isTileOccupied(candidatePosition - 8 * board.getMoveMaker().getDirection())) {
                legalMovesBitboard |= 1L << candidatePosition;
            }
        }

        return legalMovesBitboard;
    }

    private static boolean isEnPassantMove(final Board board, final int candidatePosition, final int offset, final int piecePosition) {
        final int neighbourPosition = piecePosition + (offset == 7 ? -1 : 1) * board.getMoveMaker().getDirection();

        if (!board.isTileOccupied(neighbourPosition)) {
            return false;
        }

        return !board.isTileOccupied(candidatePosition) && board.getAllianceOfTile(neighbourPosition) != board.getMoveMaker() &&
                neighbourPosition == board.getEnPassantPawnPosition();
    }

    private static boolean isContinueCase(final int offset, final int candidatePosition, final int piecePosition, final Alliance alliance) {
        return !ChessUtils.isValidPosition(candidatePosition) ||
                isFirstOrEighthColumnExclusion(piecePosition, offset) ||
                Math.abs(offset) == 16 && !isDoublePawnAdvanceable(piecePosition, alliance);
    }

    private static boolean isFirstOrEighthColumnExclusion(final int currentPosition, final int offset) {
        return ChessUtils.isPositionInColumn(currentPosition, 1) && (offset == 7 || offset == -9) ||
                ChessUtils.isPositionInColumn(currentPosition, 8) && (offset == -7 || offset == 9);
    }

    private static boolean isDoublePawnAdvanceable(final int piecePosition, final Alliance alliance) {
        return alliance.isWhite() && ChessUtils.isPositionInRow(piecePosition, 7) ||
                alliance.isBlack() && ChessUtils.isPositionInRow(piecePosition, 2);
    }

    @Override
    public Pawn movePiece(final Alliance alliance, final int toTilePosition) {
        return new Pawn(toTilePosition, alliance, false);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "P" : "p";
    }
}
