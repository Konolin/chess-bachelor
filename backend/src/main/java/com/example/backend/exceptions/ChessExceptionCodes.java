package com.example.backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChessExceptionCodes {
    TEST_EXCEPTION("test_exception"),
    ILLEGAL_STATE("illegal_state"),
    INVALID_MOVE("invalid_move"),
    INVALID_POSITION("invalid_position"),
    INVALID_FEN_STRING("invalid_fen_string");

    private final String code;
}
