package com.example.backend.models;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.pieces.*;

public class ChessUtils {
    public static final int TILES_NUMBER = 64;
    public static final int TILES_PER_ROW = 8;

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
}
