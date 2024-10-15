package com.example.backend.models.board;

import com.example.backend.models.Alliance;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.pieces.*;
import com.example.backend.models.player.Player;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final List<Tile> tiles;
    @Getter
    private final Player currentPlayer;

    private Board(final Builder builder) {
        this.tiles = createTiles(builder);
        this.currentPlayer = null;
    }

    public static Board createStartingBoard() {
        final Builder builder = new Builder();

        builder.setPieceAtPosition(new Rook(0, Alliance.BLACK))
                .setPieceAtPosition(new Knight(1, Alliance.BLACK))
                .setPieceAtPosition(new Bishop(2, Alliance.BLACK))
                .setPieceAtPosition(new Queen(3, Alliance.BLACK))
                .setPieceAtPosition(new King(4, Alliance.BLACK))
                .setPieceAtPosition(new Bishop(5, Alliance.BLACK))
                .setPieceAtPosition(new Knight(6, Alliance.BLACK))
                .setPieceAtPosition(new Rook(7, Alliance.BLACK))
                .setPieceAtPosition(new Rook(56, Alliance.WHITE))
                .setPieceAtPosition(new Knight(57, Alliance.WHITE))
                .setPieceAtPosition(new Bishop(58, Alliance.WHITE))
                .setPieceAtPosition(new Queen(59, Alliance.WHITE))
                .setPieceAtPosition(new King(60, Alliance.WHITE))
                .setPieceAtPosition(new Bishop(61, Alliance.WHITE))
                .setPieceAtPosition(new Knight(62, Alliance.WHITE))
                .setPieceAtPosition(new Rook(63, Alliance.WHITE));

        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(i + 8, Alliance.BLACK));
        }
        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(i + 48, Alliance.WHITE));
        }

        return builder.build();
    }

    private static List<Tile> createTiles(final Builder builder) {
        final Tile[] tiles = new Tile[ChessUtils.NUM_TILES];
        for (int i = 0; i < ChessUtils.NUM_TILES; i++) {
            tiles[i] = Tile.createTile(i, builder.boardConfig.get(i));
        }
        return List.of(tiles);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.NUM_TILES; i++) {
            final String tileText = this.tiles.get(i).toString();
            builder.append(String.format("%3s", tileText));
            if (((i + 1) % ChessUtils.NUM_TILES_PER_ROW) == 0) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public Tile getTileAtPosition(final int position) {
        return this.tiles.get(position);
    }

    public static class Builder {
        private final Map<Integer, Piece> boardConfig;
        private Alliance moveMaker;
        private Move transitionMove;

        public Builder() {
            this.boardConfig = new HashMap<>();
        }

        public Builder setPieceAtPosition(final Piece piece) {
            this.boardConfig.put(piece.getPosition(), piece);
            return this;
        }

        public Builder setMoveMaker(final Alliance nextMoveMaker) {
            this.moveMaker = nextMoveMaker;
            return this;
        }

        public Builder setTransitionMove(Move transitionMove) {
            this.transitionMove = transitionMove;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}
