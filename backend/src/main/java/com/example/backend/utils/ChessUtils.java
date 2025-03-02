package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.Move;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for chess-related operations such as piece creation and move filtering.
 * Also contains constants and methods for chessboard representation.
 */
public class ChessUtils {
    public static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -";

    public static final int TILES_NUMBER = 64;
    public static final int TILES_PER_ROW = 8;

    private static final String[] ALGEBRAIC_NOTATION = initializeAlgebraicNotation();

    private static final boolean[] IS_FIRST_COLUMN = initColumn(0);
    private static final boolean[] IS_EIGHTH_COLUMN = initColumn(7);

    private static final boolean[] IS_EIGHTH_ROW = initRow(56);
    private static final boolean[] IS_SEVENTH_ROW = initRow(48);
    private static final boolean[] IS_SECOND_ROW = initRow(8);
    private static final boolean[] IS_FIRST_ROW = initRow(0);

    private ChessUtils() {
        throw new ChessException("illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    /**
     * Checks if the given position is within the bounds of the chessboard.
     *
     * @param position the position to validate
     * @return {@code true} if the position is valid, {@code false} otherwise
     */
    public static boolean isValidPosition(final int position) {
        return position >= 0 && position < 64;
    }

    /**
     * Initializes a boolean array representing a specific column on the chessboard.
     *
     * @param columnNumber the index of the column (0-based)
     * @return a boolean array where {@code true} indicates tiles in the specified column
     */
    private static boolean[] initColumn(final int columnNumber) {
        final boolean[] column = new boolean[TILES_NUMBER];
        for (int i = columnNumber; i < TILES_NUMBER; i += TILES_PER_ROW) {
            column[i] = true;
        }
        return column;
    }

    /**
     * Initializes a boolean array representing a specific row on the chessboard.
     *
     * @param rowNumber the index of the first tile in the row (0-based)
     * @return a boolean array where {@code true} indicates tiles in the specified row
     */
    private static boolean[] initRow(int rowNumber) {
        final boolean[] row = new boolean[TILES_NUMBER];
        do {
            row[rowNumber++] = true;
        } while (rowNumber % TILES_PER_ROW != 0);
        return row;
    }

    /**
     * Initializes the algebraic notation for the chessboard.
     *
     * @return an array of strings representing the algebraic notation of all tiles
     */
    private static String[] initializeAlgebraicNotation() {
        return new String[]{
                "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
                "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
                "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
                "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
                "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
                "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
                "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
                "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"
        };
    }

    /**
     * Retrieves the algebraic notation for a given coordinate.
     *
     * @param coordinate the board coordinate (0-63)
     * @return the algebraic notation of the tile
     */
    public static String getAlgebraicNotationAtCoordinate(final int coordinate) {
        return ALGEBRAIC_NOTATION[coordinate];
    }

    /**
     * Filters a list of moves to exclude those that leave the player's king in check.
     *
     * @param allMoves          the list of all possible moves
     * @param piecesBitBoards   the bitboards of all pieces on the board
     * @param enPassantPosition the position of the en passant pawn, or -1 if not applicable
     * @param opponentsAlliance the alliance of the opponent
     * @return a list of valid moves that do not result in check
     */
    public static List<Move> filterMovesResultingInCheck(final List<Move> allMoves,
                                                         final PiecesBitBoards piecesBitBoards,
                                                         final int enPassantPosition,
                                                         final Alliance opponentsAlliance) {
        final List<Move> validMoves = new ArrayList<>();

        for (final Move move : allMoves) {
            // temporarily update the bitboards
            long fromTileMask = 1L << move.getFromTileIndex();
            long toTileMask = 1L << move.getToTileIndex();

            // check if the move leaves the opponent's king in check
            if (!isSquareAttacked(piecesBitBoards, fromTileMask, toTileMask, enPassantPosition,
                    opponentsAlliance, move.getMoveType().isEnPassant())) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    /**
     * Determines if a square is attacked by any opponent piece.
     *
     * @param piecesBitBoards   the bitboards of all pieces on the board
     * @param fromTileMask      the mask of the tile the piece is moving from
     * @param toTileMask        the mask of the tile the piece is moving to
     * @param enPassantPosition the position of the en passant pawn, or -1 if not applicable
     * @param opponentAlliance  the alliance of the opponent
     * @param isEnPassant       {@code true} if the move is an en passant capture, {@code false} otherwise
     * @return {@code true} if the square is attacked, {@code false} otherwise
     */
    private static boolean isSquareAttacked(
            final PiecesBitBoards piecesBitBoards,
            final long fromTileMask,
            final long toTileMask,
            final int enPassantPosition,
            final Alliance opponentAlliance,
            final boolean isEnPassant) {
        // get the mask of all the attacks from the opponent
        long allAttacksMask = 0L;
        // get the occupancy mask for the current move
        long occupancyMask = piecesBitBoards.getAllPieces() & ~fromTileMask | toTileMask;
        // remove the enPassant pawn if the move is enPassant
        if (isEnPassant) {
            occupancyMask &= ~(1L << enPassantPosition);
        }

        // add the attacks from knights
        // get the bitboard for the knight before the move was made
        long knightBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KNIGHT, opponentAlliance);
        // update the bitmask if a knight was captured
        knightBitboard &= ~toTileMask;
        // loop over all the knights and get the attacks
        while (knightBitboard != 0L) {
            final int knightPosition = Long.numberOfTrailingZeros(knightBitboard);
            allAttacksMask |= BitBoardUtils.getKnightAttackMask(knightPosition);
            knightBitboard &= knightBitboard - 1;
        }

        // add the attacks from bishops
        long bishopBitboard = piecesBitBoards.getPieceBitBoard(PieceType.BISHOP, opponentAlliance);
        bishopBitboard &= ~toTileMask;
        while (bishopBitboard != 0L) {
            final int bishopPosition = Long.numberOfTrailingZeros(bishopBitboard);
            allAttacksMask |= MagicBitBoards.getBishopAttacks(bishopPosition, occupancyMask);
            bishopBitboard &= bishopBitboard - 1;
        }

        // add the attacks from rooks
        long rookBitboard = piecesBitBoards.getPieceBitBoard(PieceType.ROOK, opponentAlliance);
        rookBitboard &= ~toTileMask;
        while (rookBitboard != 0L) {
            final int rookPosition = Long.numberOfTrailingZeros(rookBitboard);
            allAttacksMask |= MagicBitBoards.getRookAttacks(rookPosition, occupancyMask);
            rookBitboard &= rookBitboard - 1;
        }

        // add the attacks from queens
        long queenBitboard = piecesBitBoards.getPieceBitBoard(PieceType.QUEEN, opponentAlliance);
        queenBitboard &= ~toTileMask;
        while (queenBitboard != 0L) {
            final int queenPosition = Long.numberOfTrailingZeros(queenBitboard);
            allAttacksMask |= MagicBitBoards.getRookAttacks(queenPosition, occupancyMask);
            allAttacksMask |= MagicBitBoards.getBishopAttacks(queenPosition, occupancyMask);
            queenBitboard &= queenBitboard - 1;
        }

        // add the attacks from pawns
        long pawnBitboard = piecesBitBoards.getPieceBitBoard(PieceType.PAWN, opponentAlliance);
        // update the bitmask if a pawn was captured
        pawnBitboard &= isEnPassant ? ~(1L << enPassantPosition) : ~toTileMask;
        // get the mask of all the attacks from the opponent's pawns
        pawnBitboard = BitBoardUtils.calculatePawnAttackingBitboard(pawnBitboard, opponentAlliance);
        allAttacksMask |= pawnBitboard;

        // add the attacks from the king
        long kingBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KING, opponentAlliance);
        final int kingPosition = Long.numberOfTrailingZeros(kingBitboard);
        allAttacksMask |= BitBoardUtils.getKingAttackMask(kingPosition);

        // get the bitboard of friendly king
        long friendlyKingBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KING, opponentAlliance.getOpponent());
        // update the bitmask if the king made the move
        if ((friendlyKingBitboard & ~fromTileMask) == 0L) {
            friendlyKingBitboard = toTileMask;
        }
        // return if the king is on the attack mask
        return (friendlyKingBitboard & allAttacksMask) != 0L;
    }

    /**
     * Checks if the given position is in the specified row.
     *
     * @param position the position to check
     * @param row      the row to check against
     * @return {@code true} if the position is in the row, {@code false} otherwise
     */
    public static boolean isPositionInRow(final int position, final int row) {
        return switch (row) {
            case 1 -> IS_FIRST_ROW[position];
            case 2 -> IS_SECOND_ROW[position];
            case 7 -> IS_SEVENTH_ROW[position];
            case 8 -> IS_EIGHTH_ROW[position];
            default -> false;
        };
    }

    /**
     * Checks if the given position is in the specified column.
     *
     * @param position the position to check
     * @param column   the column to check against
     * @return {@code true} if the position is in the column, {@code false} otherwise
     */
    public static boolean isPositionInColumn(final int position, final int column) {
        return switch (column) {
            case 1 -> IS_FIRST_COLUMN[position];
            case 8 -> IS_EIGHTH_COLUMN[position];
            default -> false;
        };
    }

    /**
     * Check if a position is on the starting rank (7th rank for White, 2nd for Black),
     * which is needed for a double pawn push.
     */
    public static boolean isOnStartingRank(int position, Alliance alliance) {
        return alliance.isWhite() && ChessUtils.isPositionInRow(position, 7)
                || alliance.isBlack() && ChessUtils.isPositionInRow(position, 2);
    }

    public static PieceType getPieceTypeByIndex(final int index) {
        return switch (index) {
            case 0 -> PieceType.PAWN;
            case 1 -> PieceType.KNIGHT;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.ROOK;
            case 4 -> PieceType.QUEEN;
            case 5 -> PieceType.KING;
            default -> throw new ChessException("Invalid piece type", ChessExceptionCodes.INVALID_PIECE_TYPE);
        };
    }
}
