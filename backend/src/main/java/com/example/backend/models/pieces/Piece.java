package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Piece {
    private final Alliance alliance;
    private final PieceType type;
    private final int position;

    public Piece(final int position, final Alliance alliance, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.type = type;
    }

    public static List<Move> generateLegalMovesList(final Board board, final int piecePosition, final Alliance alliance, final PieceType type, long legalMovesBitBoard) {
        List<Move> legalMoves = new ArrayList<>();

        // Get the bitboard of the opponent's pieces
        long opponentPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(alliance.getOpponent());

        // Separate the attack moves (moves to opponent pieces) and normal moves (empty squares)
        long attackMoves = legalMovesBitBoard & opponentPiecesBitBoard;
        long normalMoves = legalMovesBitBoard & ~opponentPiecesBitBoard;

        if (type == PieceType.PAWN) {
            // if enPassant is possible, the target square is empty so it wouldn't appear in 'attackSquaresBB' automatically.
            // instead, if the enPassant square is in movesBB, treat it like an attack.
            int enPassantSquare = board.getEnPassantPawnPosition();
            if (enPassantSquare != -1 && ((normalMoves & (1L << enPassantSquare)) != 0)) {
                attackMoves |= (1L << enPassantSquare);
            }

            // convert the bitBoards to Move objects
            legalMoves.addAll(bitboardToPawnMoves(board, piecePosition, normalMoves, alliance, false));
            legalMoves.addAll(bitboardToPawnMoves(board, piecePosition, attackMoves, alliance, true));
        } else {
            // Convert the bitboards to Move objects and add them to the legal moves list
            legalMoves.addAll(bitBoardToMoveList(normalMoves, MoveType.NORMAL, piecePosition));
            legalMoves.addAll(bitBoardToMoveList(attackMoves, MoveType.ATTACK, piecePosition));
        }

        return legalMoves;
    }

    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition, final Alliance alliance, final PieceType type) {
        // get the occupancy bitboard for all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();

        // get all the moves of the specified piece
        long allMovesBitBoard = getAllMovesBitBoard(type, piecePosition, occupancyBitBoard, board, alliance);

        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(alliance);
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    private static long getAllMovesBitBoard(final PieceType type,
                                            final int piecePosition,
                                            final long occupancyBitBoard,
                                            final Board board,
                                            final Alliance alliance) {
        return switch (type) {
            case KING -> BitBoardUtils.getKingAttackMask(piecePosition);
            case KNIGHT -> BitBoardUtils.getKnightAttackMask(piecePosition);
            case BISHOP -> MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard);
            case ROOK -> MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
            case QUEEN -> MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard)
                    | MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
            case PAWN -> generateLegalMovesBitBoardForPawns(board, piecePosition, alliance);
        };
    }

    private static long generateLegalMovesBitBoardForPawns(final Board board, final int piecePosition, final Alliance alliance) {
        long movesBB = 0L;
        final int direction = alliance.getDirection();
        final int forwardOne = piecePosition + 8 * direction;
        final int forwardTwo = piecePosition + 16 * direction;

        // single push (must be on board, must be empty)
        if (ChessUtils.isValidPosition(forwardOne) && !board.isTileOccupied(forwardOne)) {
            movesBB |= (1L << forwardOne);

            // double push (only if on starting rank, and the forwardTwo is empty too)
            if (ChessUtils.isOnStartingRank(piecePosition, alliance)
                    && ChessUtils.isValidPosition(forwardTwo)
                    && !board.isTileOccupied(forwardTwo)) {
                movesBB |= (1L << forwardTwo);
            }
        }

        // captures (left diagonal, right diagonal).
        int leftCapture = piecePosition + (alliance.isWhite() ? -9 : 7);
        if (isValidPawnCapture(board, leftCapture, alliance, piecePosition, true)) {
            movesBB |= (1L << leftCapture);
        }

        int rightCapture = piecePosition + (alliance.isWhite() ? -7 : 9);
        if (isValidPawnCapture(board, rightCapture, alliance, piecePosition, false)) {
            movesBB |= (1L << rightCapture);
        }

        return movesBB;
    }

    private static boolean isValidPawnCapture(Board board,
                                              int candidatePos,
                                              Alliance alliance,
                                              int piecePosition,
                                              boolean isLeftDiagonal) {
        // check if position is on board
        if (!ChessUtils.isValidPosition(candidatePos)) {
            return false;
        }

        // stop moves that wrap around the board
        if (isLeftDiagonal) {
            if (ChessUtils.isPositionInColumn(piecePosition, 1)) {
                return false;
            }
        } else {
            if (ChessUtils.isPositionInColumn(piecePosition, 8)) {
                return false;
            }
        }

        // if there's an opponent piece there, it's valid
        if (board.isTileOccupied(candidatePos)
                && board.getAllianceOfTile(candidatePos) == alliance.getOpponent()) {
            return true;
        }

        // EN PASSANT check:
        // if there is an enPassant square, and the candidatePos is the square behind it
        // (+8 for black attack and -8 for white attack), it's valid.
        // else the attack is invalid as we ruled out all other cases
        int enPassantSquare = board.getEnPassantPawnPosition();
        return enPassantSquare != -1 && candidatePos == enPassantSquare + (alliance.isWhite() ? -8 : 8);
    }

    private static List<Move> bitBoardToMoveList(long bitBoard, final MoveType moveType, final int piecePosition) {
        int bitCount = Long.bitCount(bitBoard);
        List<Move> legalMoves = new ArrayList<>(bitCount);
        while (bitBoard != 0) {
            int destination = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;
            legalMoves.add(new Move(piecePosition, destination, moveType));
        }
        return legalMoves;
    }

    private static List<Move> bitboardToPawnMoves(Board board,
                                                  int piecePosition,
                                                  long squaresBB,
                                                  Alliance alliance,
                                                  boolean isCapture) {
        List<Move> moves = new ArrayList<>();

        while (squaresBB != 0) {
            int destination = Long.numberOfTrailingZeros(squaresBB);
            squaresBB &= (squaresBB - 1);

            // check if it's a promotion square
            if (alliance.isPromotionSquare(destination)) {
                for (PieceType promoType : PieceType.PROMOTABLE_TYPES) {
                    moves.add(new Move(piecePosition, destination,
                            isCapture ? MoveType.PROMOTION_ATTACK : MoveType.PROMOTION,
                            promoType));
                }
            } else {
                if (destination == board.getEnPassantPawnPosition() + (alliance.isWhite() ? -8 : 8)) {
                    moves.add(new Move(piecePosition, destination, MoveType.EN_PASSANT));
                } else if (Math.abs(destination - piecePosition) == 16) {
                    moves.add(new Move(piecePosition, destination, MoveType.DOUBLE_PAWN_ADVANCE));
                } else if (isCapture) {
                    moves.add(new Move(piecePosition, destination, MoveType.ATTACK));
                } else {
                    moves.add(new Move(piecePosition, destination, MoveType.NORMAL));
                }
            }
        }

        return moves;
    }

    @Override
    public String toString() {
        return alliance.isWhite() ? type.getAlgebraicSymbol() : type.getAlgebraicSymbol().toLowerCase();
    }
}
