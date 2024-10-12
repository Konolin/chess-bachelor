package com.example.backend.exceptions;

import lombok.Getter;

@Getter
public class ChessException extends RuntimeException {
    private final ExceptionCodes code;

    public ChessException(String message, ExceptionCodes code) {
        super(message);
        this.code = code;
    }
}
