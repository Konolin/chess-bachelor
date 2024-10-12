package com.example.backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExceptionCodes {
    TEST_EXCEPTION("test_exception"),
    ILLEGAL_STATE("illegal_state"),;

    private final String code;
}
