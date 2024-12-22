package com.example.backend.models.moves;

/**
 * The MoveType enum represents the different types of chess moves that can occur during a game.
 * Each type defines a specific move action, such as a regular move, an attack, castling, en passant, or promotion.
 */
public enum MoveType {
    NORMAL,
    ATTACK,
    EN_PASSANT,
    DOUBLE_PAWN_ADVANCE,
    PROMOTION,
    PROMOTION_ATTACK,
    KING_SIDE_CASTLE,
    QUEEN_SIDE_CASTLE;

    /**
     * Checks if the move type is a castling move (either king-side or queen-side).
     *
     * @return true if the move is a castling move, false otherwise.
     */
    public boolean isCastleMove() {
        return this == KING_SIDE_CASTLE || this == QUEEN_SIDE_CASTLE;
    }

    /**
     * Checks if the move type involves promotion (either a normal promotion or a promotion during an attack).
     *
     * @return true if the move is a promotion move, false otherwise.
     */
    public boolean isPromotion() {
        return this == PROMOTION || this == PROMOTION_ATTACK;
    }

    /**
     * Checks if the move type is a king-side castling move.
     *
     * @return true if the move is a king-side castling move, false otherwise.
     */
    public boolean isKingSideCastle() {
        return this == KING_SIDE_CASTLE;
    }

    /**
     * Checks if the move type is an attack move (either a regular attack or a promotion attack).
     *
     * @return true if the move is an attack move, false otherwise.
     */
    public boolean isAttack() {
        return this == ATTACK || this == PROMOTION_ATTACK;
    }

    /**
     * Checks if the move type is an en passant move.
     *
     * @return true if the move is an en passant move, false otherwise.
     */
    public boolean isEnPassant() {
        return this == EN_PASSANT;
    }
}
