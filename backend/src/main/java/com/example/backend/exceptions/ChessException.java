package com.example.backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChessException extends RuntimeException {
    private final ExceptionCodes code;
}
