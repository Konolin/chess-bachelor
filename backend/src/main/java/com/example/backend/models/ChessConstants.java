package com.example.backend.models;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ExceptionCodes;

public final class ChessConstants {
    public static final String STARTING_BOARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";
    public static final Integer TOTAL_TILES = 64;
    public static final Integer TILES_PER_ROW = 8;
    public static final Integer TILES_PER_COLUMN = 8;


    private ChessConstants() {
        throw new ChessException("This class cannot be instantiated", ExceptionCodes.ILLEGAL_STATE);
    }
}
