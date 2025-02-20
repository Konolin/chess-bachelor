package com.example.backend.models.board;

import com.example.backend.utils.ChessUtils;
import com.example.backend.models.pieces.Piece;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * The Tile class represents a single square on a chessboard.
 * Each tile can either be empty or occupied by a chess piece.
 * This class helps in modeling the board and managing the state of individual tiles.
 */
@Getter
public class Tile {
    /**
     * A cache of all possible empty tiles, indexed by their position on the board.
     * This is used to avoid recreating empty tiles multiple times.
     */
    private static final Map<Integer, Tile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();
    private final Piece occupyingPiece;
    private final int position;

    /**
     * Private constructor for creating a Tile with a specific occupying piece and position.
     *
     * @param occupyingPiece The piece occupying the tile, can be null if the tile is empty.
     * @param position The position of the tile on the chessboard.
     */
    private Tile(final Piece occupyingPiece, final int position) {
        this.occupyingPiece = occupyingPiece;
        this.position = position;
    }

    /**
     * Private constructor for creating an empty Tile at a given position.
     *
     * @param position The position of the tile on the chessboard.
     */
    private Tile(final int position) {
        this.occupyingPiece = null;
        this.position = position;
    }

    /**
     * Creates and returns a map of all possible empty tiles on the chessboard, indexed by their position.
     * This method is called once to initialize the cache of empty tiles.
     *
     * @return A map of empty tiles where the key is the tile position, and the value is the Tile object.
     */
    private static Map<Integer, Tile> createAllPossibleEmptyTiles() {
        final Map<Integer, Tile> emptyTileMap = new HashMap<>();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            emptyTileMap.put(i, new Tile(i));
        }
        return emptyTileMap;
    }

    /**
     * Factory method to create a Tile with a given piece and position.
     * If the piece is null, it returns a reference to an empty tile from the cache.
     *
     * @param piece The piece occupying the tile, can be null for an empty tile.
     * @param position The position of the tile on the chessboard.
     * @return A Tile instance with the specified piece and position.
     */
    public static Tile createTile(final Piece piece, final int position) {
        return piece == null ? EMPTY_TILES_CACHE.get(position) : new Tile(piece, position);
    }

    /**
     * Checks if the tile is empty (i.e., it is not occupied by any piece).
     *
     * @return true if the tile is empty, false if the tile is occupied by a piece.
     */
    public boolean isEmpty() {
        return this.occupyingPiece == null;
    }

    /**
     * Checks if the tile is occupied by a piece.
     *
     * @return true if the tile is occupied by a piece, false if it is empty.
     */
    public boolean isOccupied() {
        return !isEmpty();
    }

    /**
     * Gets the empty tile for a given position from the cache.
     * @param position The position of the empty tile.
     * @return The empty tile at the specified position.
     */
    public static Tile getEmptyTileForPosition(final int position) {
        return EMPTY_TILES_CACHE.get(position);
    }

    /**
     * Provides a string representation of the tile.
     * If the tile is empty, it returns a "-" symbol; otherwise, it returns the string representation of the occupying piece.
     *
     * @return A string representation of the tile.
     */
    @Override
    public String toString() {
        return this.occupyingPiece == null ? "-" : this.occupyingPiece.toString();
    }
}
