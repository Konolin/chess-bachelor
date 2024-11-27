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
    INVALID_FEN_STRING("invalid_fen_string"),
    INVALID_PIECE_CHARACTER("invalid_piece_character"),
    INVALID_PIECE_TYPE("invalid_piece_type"),;

    private final String code;
}
