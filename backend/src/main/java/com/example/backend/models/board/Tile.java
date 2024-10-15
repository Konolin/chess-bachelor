package com.example.backend.models.board;

import com.example.backend.models.ChessConstants;
import com.example.backend.models.pieces.Piece;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class Tile {
    private static final Map<Integer, EmptyTile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();
    private final int tileCoordinate;

    private Tile(final int tileCoordinate) {
        this.tileCoordinate = tileCoordinate;
    }

    private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
        final Map<Integer, EmptyTile> emptyTileMap = new HashMap<>();
        for (int i = 0; i < ChessConstants.TOTAL_TILES; i++) {
            emptyTileMap.put(i, new EmptyTile(i));
        }
        return emptyTileMap;
    }

    public static Tile createTile(final int tileCoordinate, final Piece piece) {
        return piece != null ? new OccupiedTile(tileCoordinate, piece) : EMPTY_TILES_CACHE.get(tileCoordinate);
    }

    public abstract boolean isTileOccupied();

    public abstract Piece getPieceOnTile();

    public static final class EmptyTile extends Tile {
        private EmptyTile(final int tileCoordinate) {
            super(tileCoordinate);
        }

        @Override
        public String toString() {
            return "-";
        }

        @Override
        public boolean isTileOccupied() {
            return false;
        }

        @Override
        public Piece getPieceOnTile() {
            return null;
        }
    }


    @Getter
    public static final class OccupiedTile extends Tile {
        private final Piece pieceOnTile;

        private OccupiedTile(final int tileCoordinate, final Piece pieceOnTile) {
            super(tileCoordinate);
            this.pieceOnTile = pieceOnTile;
        }

//        TODO - complete this
//        @Override
//        public String toString() {
//            // black pieces <=> lower case; white pieces <=> upper case
//            return getPieceOnTile().getPieceAlliance().isBlack() ? getPieceOnTile().toString().toLowerCase() : getPieceOnTile().toString();
//        }

        @Override
        public boolean isTileOccupied() {
            return true;
        }
    }
}