package com.example.backend.models.pieces;

import lombok.Getter;

/**
 * Enum representing the different types of chess pieces.
 * Each piece type corresponds to a specific type of chess piece (e.g., Pawn, Knight, Bishop, etc.).
 * The enum provides a name and an algebraic symbol for each piece type.
 */
@Getter
public enum PieceType {
    PAWN("Pawn", 0, 100),
    KNIGHT("Knight", 1, 300),
    BISHOP("Bishop", 2, 300),
    ROOK("Rook", 3, 500),
    QUEEN("Queen", 4, 900),
    KING("King", 5, 0);

    public static final PieceType[] ALL_TYPES = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN, PieceType.KING};
    static final PieceType[] PROMOTABLE_TYPES = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN};
    private final String name;
    private final String algebraicSymbol;
    private final int index;
    private int value;

    PieceType(final String name, final int index, final int value) {
        this.name = name;
        this.algebraicSymbol = name.substring(0, 1);
        this.index = index;
        this.value = value;
    }
}
