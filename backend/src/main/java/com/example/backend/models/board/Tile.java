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
    private final int tilePosition;

    private Tile(final Piece occupyingPiece, final int tilePosition) {
        this.occupyingPiece = occupyingPiece;
        this.tilePosition = tilePosition;
    }

    @Override
    public String toString() {
        return this.occupyingPiece != null ? this.occupyingPiece.toString() : "-";
    }

    private static Map<Integer, Tile> createAllPossibleEmptyTiles() {
        final Map<Integer, Tile> emptyTileMap = new HashMap<>();
        for (int i = 0; i < ChessUtils.NUM_TILES; i++) {
            emptyTileMap.put(i, new Tile(null, i));
        }
        return emptyTileMap;
    }

    public static Tile createTile(final int tileCoordinate, final Piece piece) {
        return piece != null ? new Tile(piece, tileCoordinate) : EMPTY_TILES_CACHE.get(tileCoordinate);
    }

    public boolean isTileOccupied() {
        return this.occupyingPiece != null;
    }
}
