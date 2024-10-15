package com.example.backend.models.pieces;

import com.example.backend.models.Color;
import lombok.Getter;

@Getter
public abstract class Piece {
    private final Color color;
    private final Integer position;

    Piece(Color color, Integer position) {
        this.color = color;
        this.position = position;
    }
}
