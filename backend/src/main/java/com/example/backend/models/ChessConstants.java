package com.example.backend.models;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ExceptionCodes;

public final class ChessConstants {
    public static String STARTING_BOARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    private ChessConstants() {
        throw new ChessException("This class cannot be instantiated", ExceptionCodes.ILLEGAL_STATE);
    }
}
