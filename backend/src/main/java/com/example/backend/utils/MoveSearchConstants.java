package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;

public class MoveSearchConstants {
    public static final int MAX_DEPTH = 30;
    public static final int TT_SIZE = 4 * 1024 * 1024; // 4M entries
    public static final int MAX_KILLER_MOVES = 2;
    public static final int ASPIRATION_WINDOW = 25; // In centi-pawns
    public static final float TIME_BUFFER_FACTOR = 0.8f;

    private MoveSearchConstants() {
        throw new ChessException("Utility class", ChessExceptionCodes.ILLEGAL_STATE);
    }
}
