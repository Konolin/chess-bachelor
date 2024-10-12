package com.example.backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExceptionCodes {
    TEST_EXCEPTION("test_exception"),;

    private final String code;
}
