package com.example.backend.models.pieces;

import com.example.backend.models.Alliance;
import lombok.Getter;

@Getter
public abstract class Piece {
    private final int position;
    private final Alliance alliance;

    Piece(final int position, final Alliance alliance) {
        this.position = position;
        this.alliance = alliance;
    }
}
