package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.moves.MoveType;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;
import com.example.backend.utils.MoveUtils;
import lombok.Getter;

/**
 * The Piece class represents a chess piece on the board.
 * It encapsulates information about the piece's type, position, and alliance (color).
 * It also provides methods for generating legal moves for the piece.
 */
@Getter
public class Piece {
    private final Alliance alliance;
    private final PieceType type;
    private final int position;

    /**
     * Creates a new Piece object with the specified position, alliance, and type.
     *
     * @param position The position of the piece on the board.
     * @param alliance The alliance (color) of the piece.
     * @param type     The type of the piece (e.g., Pawn, Rook, Knight).
     */
    public Piece(final int position, final Alliance alliance, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.type = type;
    }

    /**
     * Generates a list of legal moves for the given piece on the board.
     * The legal moves are based on the current board state and the piece's position.
     *
     * @param board              The current board state.
     * @param piecePosition      The position of the piece on the board.
     * @param alliance           The alliance (color) of the piece.
     * @param type               The type of the piece (e.g., Pawn, Rook, Knight).
     * @param legalMovesBitBoard The bitboard representing all possible legal moves for the piece.
     * @return A list of legal moves for the piece.
     */
    public static MoveList generateLegalMovesList(final Board board,
                                                  final int piecePosition,
                                                  final Alliance alliance,
                                                  final PieceType type,
                                                  final long legalMovesBitBoard) {
        MoveList legalMoves = new MoveList();

        // get the bitboard of the opponent's pieces
        long opponentPiecesBitBoard = board.getPiecesBBs().getAllianceBitBoard(alliance.getOpponent());

        // separate the attack moves and normal moves (to empty squares)
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
            // convert the bitboards to Move objects and add them to the legal moves list
            legalMoves.addAll(bitBoardToMoveList(normalMoves, MoveType.NORMAL, piecePosition));
            legalMoves.addAll(bitBoardToMoveList(attackMoves, MoveType.ATTACK, piecePosition));
        }

        return legalMoves;
    }

    /**
     * Generates a bitboard representing all possible legal moves for the given piece on the board.
     * This method takes all moves possible for a piece on the given board and filters out moves that
     * are blocked by friendly pieces.
     *
     * @param board         the current board
     * @param piecePosition the position of the piece
     * @param alliance      the alliance of the piece
     * @param type          the type of the piece
     * @return a bitboard representing all possible legal moves for the piece
     */
    public static long generateLegalMovesBitBoard(final Board board,
                                                  final int piecePosition,
                                                  final Alliance alliance,
                                                  final PieceType type) {
        // get the occupancy bitboard for all pieces on the board
        long occupancyBitBoard = board.getPiecesBBs().getAllPieces();

        // get all the moves of the specified piece
        long allMovesBitBoard = getAllMovesBitBoard(type, piecePosition, occupancyBitBoard, board, alliance);

        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBBs().getAllianceBitBoard(alliance);
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    /**
     * Generates a bitboard representing all possible moves for the given piece on the board.
     * This includes both legal and illegal moves, such as moves that put the king in check
     * and capture of friendly pieces.
     *
     * @param type              the type of the piece
     * @param piecePosition     the position of the piece
     * @param occupancyBitBoard the occupancy bitboard for all pieces on the board
     * @param board             the current board
     * @param alliance          the alliance of the piece
     * @return a bitboard representing all possible moves for the piece
     */
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

    /**
     * Generates a bitboard representing all possible moves for pawns on the board.
     *
     * @param board         the current board
     * @param piecePosition the position of the pawn
     * @param alliance      the alliance of the pawn
     * @return a bitboard representing all possible moves for the pawn
     */
    private static long generateLegalMovesBitBoardForPawns(final Board board,
                                                           final int piecePosition,
                                                           final Alliance alliance) {
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

    /**
     * Checks if a pawn capture is valid. A pawn capture is valid if:
     * - The candidate position is on the board.
     * - The candidate position is not on the left edge of the board (if it's a left diagonal capture).
     * - The candidate position is not on the right edge of the board (if it's a right diagonal capture).
     * - There is an opponent piece at the candidate position.
     * - The candidate position is the en passant square.
     *
     * @param board          the current board
     * @param candidatePos   the candidate position
     * @param alliance       the alliance of the pawn
     * @param piecePosition  the position of the pawn
     * @param isLeftDiagonal true if it's a left diagonal capture, false if it's a right diagonal capture
     * @return true if the pawn capture is valid, false otherwise
     */
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
                && board.getPieceAllianceAtPosition(candidatePos) == alliance.getOpponent()) {
            return true;
        }

        // EN PASSANT check:
        // if there is an enPassant square, and the candidatePos is the square behind it
        // (+8 for black attack and -8 for white attack), it's valid.
        // else the attack is invalid as we ruled out all other cases
        int enPassantSquare = board.getEnPassantPawnPosition();
        return enPassantSquare != -1 && candidatePos == enPassantSquare + (alliance.isWhite() ? -8 : 8);
    }

    /**
     * Converts a bitboard to a list of Move objects.
     * Works for all pieces except pawns.
     *
     * @param bitBoard the bitboard representing the possible moves
     * @param moveType the type of the move (e.g., normal, attack)
     * @param piecePosition the position of the piece
     * @return a list of Move objects representing the possible moves
     */
    private static MoveList bitBoardToMoveList(long bitBoard, final MoveType moveType, final int piecePosition) {
        MoveList legalMoves = new MoveList();
        while (bitBoard != 0) {
            int destination = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;
            legalMoves.add(MoveUtils.createMove(piecePosition, destination, moveType, null));
        }
        return legalMoves;
    }

    /**
     * Converts a bitboard representing possible pawn moves to a list of Move objects.
     *
     * @param board the current board
     * @param piecePosition the position of the pawn
     * @param squaresBB the bitboard representing the possible moves
     * @param alliance the alliance of the pawn
     * @param isCapture true if the move is a capture, false otherwise
     * @return a list of Move objects representing the possible moves
     */
    private static MoveList bitboardToPawnMoves(Board board,
                                                  int piecePosition,
                                                  long squaresBB,
                                                  Alliance alliance,
                                                  boolean isCapture) {
        MoveList moves = new MoveList();

        while (squaresBB != 0) {
            int destination = Long.numberOfTrailingZeros(squaresBB);
            squaresBB &= (squaresBB - 1);

            // check if it's a promotion square
            if (alliance.isPromotionSquare(destination)) {
                for (PieceType promoType : PieceType.PROMOTABLE_TYPES) {
                    moves.add(MoveUtils.createMove(piecePosition, destination, isCapture ? MoveType.PROMOTION_ATTACK : MoveType.PROMOTION, promoType));
                }
            } else {
                if (destination == board.getEnPassantPawnPosition() + (alliance.isWhite() ? -8 : 8)) {
                    moves.add(MoveUtils.createMove(piecePosition, destination, MoveType.EN_PASSANT, null));
                } else if (Math.abs(destination - piecePosition) == 16) {
                    moves.add(MoveUtils.createMove(piecePosition, destination, MoveType.DOUBLE_PAWN_ADVANCE, null));
                } else if (isCapture) {
                    moves.add(MoveUtils.createMove(piecePosition, destination, MoveType.ATTACK, null));
                } else {
                    moves.add(MoveUtils.createMove(piecePosition, destination, MoveType.NORMAL, null));
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
