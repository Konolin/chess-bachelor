package com.example.backend.exceptions;

import lombok.Getter;

@Getter
public class ChessException extends RuntimeException {
    private final ChessExceptionCodes code;

    public ChessException(String message, ChessExceptionCodes code) {
        super(message);
        this.code = code;
    }
}
