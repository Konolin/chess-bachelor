package com.example.backend.models.board;

import com.example.backend.models.Alliance;
import com.example.backend.models.pieces.Piece;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final List<Tile> tiles;

    private Board(final Builder builder) {
        this.tiles = createTiles();
    }

    private List<Tile> createTiles() {

    }

    public static Board createStartingBoard() {
        final Builder builder = new Builder();

        builder.setPieceAtPosition(new Rook(Alliance.BLACK, 0))
                .setPieceAtPosition(new Knight(Alliance.BLACK, 1))
                .setPieceAtPosition(new Bishop(Alliance.BLACK, 2))
                .setPieceAtPosition(new Queen(Alliance.BLACK, 3))
                .setPieceAtPosition(new King(Alliance.BLACK, 4, true, true))
                .setPieceAtPosition(new Bishop(Alliance.BLACK, 5))
                .setPieceAtPosition(new Knight(Alliance.BLACK, 6))
                .setPieceAtPosition(new Rook(Alliance.BLACK, 7))
                .setPieceAtPosition(new Rook(Alliance.WHITE, 56))
                .setPieceAtPosition(new Knight(Alliance.WHITE, 57))
                .setPieceAtPosition(new Bishop(Alliance.WHITE, 58))
                .setPieceAtPosition(new Queen(Alliance.WHITE, 59))
                .setPieceAtPosition(new King(Alliance.WHITE, 60, true, true))
                .setPieceAtPosition(new Bishop(Alliance.WHITE, 61))
                .setPieceAtPosition(new Knight(Alliance.WHITE, 62))
                .setPieceAtPosition(new Rook(Alliance.WHITE, 63));

        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(Alliance.BLACK, i + 8));
        }
        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(Alliance.WHITE, i + 48));
        }

        return builder.build();
    }

    public static class Builder {
        private final Map<Integer, Piece> boardConfig;

        public Builder () {
            this.boardConfig = new HashMap<>();
        }

        public Builder setPieceAtPosition(final Piece piece) {
            this.boardConfig.put(piece.getPosition(), piece);
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}
