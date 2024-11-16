package com.example.backend.models;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
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

    public static void initializeStandardPosition(Board.Builder builder) {
        // Set up black pawns
        for (int i = 8; i < 16; i++) {
            builder.setPieceAtPosition(new Pawn(i, Alliance.BLACK, true));
        }

        // Set up white pawns
        for (int i = 48; i < 56; i++) {
            builder.setPieceAtPosition(new Pawn(i, Alliance.WHITE, true));
        }

        // Set up black pieces
        builder.setPieceAtPosition(new Rook(0, Alliance.BLACK, true))
                .setPieceAtPosition(new Knight(1, Alliance.BLACK))
                .setPieceAtPosition(new Bishop(2, Alliance.BLACK))
                .setPieceAtPosition(new Queen(3, Alliance.BLACK))
                .setPieceAtPosition(new King(4, Alliance.BLACK, true))
                .setPieceAtPosition(new Bishop(5, Alliance.BLACK))
                .setPieceAtPosition(new Knight(6, Alliance.BLACK))
                .setPieceAtPosition(new Rook(7, Alliance.BLACK, true));

        // Set up white pieces
        builder.setPieceAtPosition(new Rook(56, Alliance.WHITE, true))
                .setPieceAtPosition(new Knight(57, Alliance.WHITE))
                .setPieceAtPosition(new Bishop(58, Alliance.WHITE))
                .setPieceAtPosition(new Queen(59, Alliance.WHITE))
                .setPieceAtPosition(new King(60, Alliance.WHITE, true))
                .setPieceAtPosition(new Bishop(61, Alliance.WHITE))
                .setPieceAtPosition(new Knight(62, Alliance.WHITE))
                .setPieceAtPosition(new Rook(63, Alliance.WHITE, true));

        // Set en passant pawn as null for starting position
        builder.setEnPassantPawn(null);
    }

    public static List<Move> filterMovesResultingInCheck(final List<Move> allMoves, final Board board) {
        final List<Move> legalMoves = new ArrayList<>();

        for (final Move move : allMoves) {
            Board transitionBoard = board.executeMove(move);

            // remove moves that cause the current player to be in check.
            // the opponents alliance is checked because the move maker changes after executeMove(), which means
            // the current move maker (who's moves are filtered) is considered the opponent in the transitionBoard
            if (!transitionBoard.isAllianceInCheck(transitionBoard.getMoveMaker().getOpponent())) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }
}
