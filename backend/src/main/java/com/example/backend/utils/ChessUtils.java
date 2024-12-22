package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.pieces.*;

import java.util.ArrayList;
import java.util.List;

public class ChessUtils {
    public static final int TILES_NUMBER = 64;
    public static final int TILES_PER_ROW = 8;

    public static final String[] ALGEBRAIC_NOTATION = initializeAlgebraicNotation();

    public static final boolean[] FIRST_COLUMN = initColumn(0);
    public static final boolean[] SECOND_COLUMN = initColumn(1);
    public static final boolean[] SEVENTH_COLUMN = initColumn(6);
    public static final boolean[] EIGHTH_COLUMN = initColumn(7);

    public static final boolean[] EIGHTH_ROW = initRow(56);
    public static final boolean[] SEVENTH_ROW = initRow(48);
    public static final boolean[] SECOND_ROW = initRow(8);
    public static final boolean[] FIRST_ROW = initRow(0);

    private ChessUtils() {
        throw new ChessException("illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static boolean isValidPosition(final int position) {
        return position >= 0 && position < 64;
    }

    public static Piece createPieceFromCharAndPosition(final String pieceChar, final int position) {
        return switch (pieceChar) {
            case "q" -> new Queen(position, Alliance.BLACK);
            case "r" -> new Rook(position, Alliance.BLACK, false);
            case "n" -> new Knight(position, Alliance.BLACK);
            case "b" -> new Bishop(position, Alliance.BLACK);
            case "Q" -> new Queen(position, Alliance.WHITE);
            case "R" -> new Rook(position, Alliance.WHITE, false);
            case "N" -> new Knight(position, Alliance.WHITE);
            case "B" -> new Bishop(position, Alliance.WHITE);
            default ->
                    throw new ChessException("Invalid piece character " + pieceChar, ChessExceptionCodes.INVALID_PIECE_CHARACTER);
        };
    }

    public static Piece createPieceFromTypeAndPosition(final PieceType type, final Alliance alliance, final int position) {
        return switch (type) {
            case QUEEN -> new Queen(position, alliance);
            case ROOK -> new Rook(position, alliance, false);
            case KNIGHT -> new Knight(position, alliance);
            case BISHOP -> new Bishop(position, alliance);
            case PAWN -> new Pawn(position, alliance, false);
            case KING -> new King(position, alliance, false);
        };
    }

    private static boolean[] initColumn(final int columnNumber) {
        final boolean[] column = new boolean[TILES_NUMBER];
        for (int i = columnNumber; i < TILES_NUMBER; i += TILES_PER_ROW) {
            column[i] = true;
        }
        return column;
    }

    private static boolean[] initRow(int rowNumber) {
        final boolean[] row = new boolean[TILES_NUMBER];
        do {
            row[rowNumber++] = true;
        } while (rowNumber % TILES_PER_ROW != 0);
        return row;
    }

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

    public static String getAlgebraicNotationAtCoordinate(final int coordinate) {
        return ALGEBRAIC_NOTATION[coordinate];
    }

    public static List<Move> filterMovesResultingInCheck(final List<Move> allMoves, final Board board) {
        final List<Move> validMoves = new ArrayList<>();

        for (final Move move : allMoves) {
            // temporarily update the bitboards
            long fromTileMask = 1L << move.getFromTileIndex();
            long toTileMask = 1L << move.getToTileIndex();

            // check if the move leaves the opponent's king in check
            if (!isSquareAttacked(board, fromTileMask, toTileMask, move.getMoveType().isEnPassant())) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    // Check if a given square is attacked
    private static boolean isSquareAttacked(
            final Board board,
            final long fromTileMask,
            final long toTileMask,
            final boolean isEnPassant)
    {
        final PiecesBitBoards piecesBitBoards = board.getPiecesBitBoards();
        final Alliance opponentAlliance = board.getMoveMaker().getOpponent();
        // get the mask of all the attacks from the opponent
        long allAttacksMask = 0L;
        // get the occupancy mask for the current move
        long occupancyMask = piecesBitBoards.getAllPieces() & ~fromTileMask | toTileMask;
        // remove the enPassant pawn if the move is enPassant
        if (isEnPassant) {
            final int enPassantPawnPosition = board.getEnPassantPawn().getPosition();
            occupancyMask &= ~(1L << enPassantPawnPosition);
        }

        // add the attacks from knights
        // get the bitboard for the knight before the move was made
        long knightBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KNIGHT, opponentAlliance);
        // update the bitmask if a knight was captured
        knightBitboard &= ~toTileMask;
        // loop over all the knights and get the attacks
        while (knightBitboard != 0L) {
            final int knightPosition = BitBoardUtils.getLs1bIndex(knightBitboard);
            allAttacksMask |= BitBoardUtils.KNIGHT_ATTACK_MASK[knightPosition];
            knightBitboard &= knightBitboard - 1;
        }

        // add the attacks from bishops
        long bishopBitboard = piecesBitBoards.getPieceBitBoard(PieceType.BISHOP, opponentAlliance);
        bishopBitboard &= ~toTileMask;
        while (bishopBitboard != 0L) {
            final int bishopPosition = BitBoardUtils.getLs1bIndex(bishopBitboard);
            allAttacksMask |= MagicBitBoards.getBishopAttacks(bishopPosition, occupancyMask);
            bishopBitboard &= bishopBitboard - 1;
        }

        // add the attacks from rooks
        long rookBitboard = piecesBitBoards.getPieceBitBoard(PieceType.ROOK, opponentAlliance);
        rookBitboard &= ~toTileMask;
        while (rookBitboard != 0L) {
            final int rookPosition = BitBoardUtils.getLs1bIndex(rookBitboard);
            allAttacksMask |= MagicBitBoards.getRookAttacks(rookPosition, occupancyMask);
            rookBitboard &= rookBitboard - 1;
        }

        // add the attacks from queens
        long queenBitboard = piecesBitBoards.getPieceBitBoard(PieceType.QUEEN, opponentAlliance);
        queenBitboard &= ~toTileMask;
        while (queenBitboard != 0L) {
            final int queenPosition = BitBoardUtils.getLs1bIndex(queenBitboard);
            allAttacksMask |= MagicBitBoards.getRookAttacks(queenPosition, occupancyMask);
            allAttacksMask |= MagicBitBoards.getBishopAttacks(queenPosition, occupancyMask);
            queenBitboard &= queenBitboard - 1;
        }

        // add the attacks from pawns
        long pawnBitboard = piecesBitBoards.getPieceBitBoard(PieceType.PAWN, opponentAlliance);
        // update the bitmask if a pawn was captured
        pawnBitboard &= isEnPassant ? ~(1L << board.getEnPassantPawn().getPosition()) : ~toTileMask;
        // get the mask of all the attacks from the opponent's pawns
        pawnBitboard = BitBoardUtils.calculatePawnAttackingBitboard(pawnBitboard, opponentAlliance);
        allAttacksMask |= pawnBitboard;

        // add the attacks from the king
        long kingBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KING, opponentAlliance);
        final int kingPosition = BitBoardUtils.getLs1bIndex(kingBitboard);
        allAttacksMask |= BitBoardUtils.KING_ATTACK_MASK[kingPosition];

        // get the bitboard of friendly king
        long friendlyKingBitboard = piecesBitBoards.getPieceBitBoard(PieceType.KING, board.getMoveMaker());
        // update the bitmask if the king made the move
        if ((friendlyKingBitboard & ~fromTileMask) == 0L) {
            friendlyKingBitboard = toTileMask;
        }
        // return if the king is on the attack mask
        return (friendlyKingBitboard & allAttacksMask) != 0L;
    }
}
