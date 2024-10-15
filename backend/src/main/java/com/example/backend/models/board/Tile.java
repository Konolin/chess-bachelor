package com.example.backend.models.board;

import com.example.backend.models.pieces.Piece;

public class Tile {
    private final Piece occupyingPiece;
    private final int tilePosition;

    public Tile(final Piece occupyingPiece, final int tilePosition) {
        this.occupyingPiece = occupyingPiece;
        this.tilePosition = tilePosition;
    }
}
