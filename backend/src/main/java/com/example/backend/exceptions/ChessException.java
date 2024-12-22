package com.example.backend.exceptions;

import lombok.Getter;

/**
 * Custom exception class for handling chess-related errors.
 * Extends {@link RuntimeException} to allow unchecked exceptions.
 */
@Getter
public class ChessException extends RuntimeException {
    private final ChessExceptionCodes code;

    /**
     * Constructor to create a new ChessException with a detailed message and an error code.
     *
     * @param message The detailed error message describing the exception.
     * @param code    The specific error code from {@link ChessExceptionCodes}.
     */
    public ChessException(final String message, final ChessExceptionCodes code) {
        super(message);
        this.code = code;
    }
}
