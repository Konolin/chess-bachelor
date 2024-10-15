package com.example.backend.models;

public class ChessUtils {
    public static final String STARTING_BOARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static final int NUM_TILES = 64;
    public static final int NUM_TILES_PER_ROW = 8;

    public static final boolean[] EIGHTH_ROW = initRow(0);
    public static final boolean[] FIRST_ROW = initRow(56);

    public static boolean isValidTileCoordinate(final int coordinate) {
        return coordinate >= 0 && coordinate < NUM_TILES;
    }

    private static boolean[] initRow(int rowNumber) {
        final boolean[] row = new boolean[NUM_TILES];
        do {
            row[rowNumber++] = true;
        } while (rowNumber % NUM_TILES_PER_ROW != 0);
        return row;
    }
}
