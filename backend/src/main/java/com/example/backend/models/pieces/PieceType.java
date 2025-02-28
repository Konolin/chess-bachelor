package com.example.backend.models.pieces;

import lombok.Getter;

/**
 * Enum representing the different types of chess pieces.
 * Each piece type corresponds to a specific type of chess piece (e.g., Pawn, Knight, Bishop, etc.).
 * The enum provides a name and an algebraic symbol for each piece type.
 */
@Getter
public enum PieceType {
    PAWN("Pawn"),
    KNIGHT("Knight"),
    BISHOP("Bishop"),
    ROOK("Rook"),
    QUEEN("Queen"),
    KING("King");

    static final PieceType[] PROMOTABLE_TYPES = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN};
    private final String name;
    private final String algebraicSymbol;

    PieceType(String name) {
        this.name = name;
        this.algebraicSymbol = name.substring(0, 1);
    }
}
