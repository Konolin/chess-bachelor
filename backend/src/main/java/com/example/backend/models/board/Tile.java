package com.example.backend.models.board;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.pieces.Piece;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Tile {
    private static final Map<Integer, Tile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();

    private final Piece occupyingPiece;
    private final int position;

    private Tile(final Piece occupyingPiece, final int position) {
        this.occupyingPiece = occupyingPiece;
        this.position = position;
    }

    private Tile(final int position) {
        this.occupyingPiece = null;
        this.position = position;
    }

    private static Map<Integer, Tile> createAllPossibleEmptyTiles() {
        final Map<Integer, Tile> emptyTileMap = new HashMap<>();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            emptyTileMap.put(i, new Tile(i));
        }
        return emptyTileMap;
    }

    public static Tile createTile(final Piece piece, final int position) {
        return piece == null ? EMPTY_TILES_CACHE.get(position) : new Tile(piece, position);
    }

    public boolean isEmpty() {
        return this.occupyingPiece == null;
    }

    @Override
    public String toString() {
        return this.occupyingPiece == null ? "-" : this.occupyingPiece.toString();
    }
}
