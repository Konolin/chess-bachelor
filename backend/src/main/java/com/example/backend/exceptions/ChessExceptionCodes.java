package com.example.backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration representing specific error codes for Chess exceptions.
 * Each error code is associated with a descriptive string for easy identification.
 */
@AllArgsConstructor
@Getter
public enum ChessExceptionCodes {
    /**
     * Indicates an illegal state in the chess engine or application.
     */
    ILLEGAL_STATE("illegal_state"),
    /**
     * Represents an invalid chess move that does not comply with the rules.
     */
    INVALID_MOVE("invalid_move"),
    /**
     * Indicates an invalid position on the chessboard.
     */
    INVALID_POSITION("invalid_position"),
    /**
     * Represents an invalid FEN (Forsyth-Edwards Notation) string.
     */
    INVALID_FEN_STRING("invalid_fen_string"),
    /**
     * Represents an invalid type for a chess piece.
     */
    INVALID_PIECE_TYPE("invalid_piece_type"),
    /**
     * Indicates that a king piece was not found during a critical operation.
     */
    KING_NOT_FOUND("king_not_found"),
    /**
     * Indicates that the model failed to load properly.
     */
    FAILED_TO_LOAD_MODEL("failed_to_load_model"),
    /**
     * Indicates that the model failed to make an inference.
     */
    FAILED_INFERENCE("failed_inference");
    // The string code associated with each error type for easier identification.
    private final String code;
}