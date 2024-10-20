package com.example.backend.models;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ExceptionCodes;

public class ChessUtils {
    public static final int TILES_NUMBER = 64;
    public static final int TILES_PER_ROW = 8;

    public static final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    private ChessUtils() {
        throw new ChessException("illegal state", ExceptionCodes.ILLEGAL_STATE);
    }
}
