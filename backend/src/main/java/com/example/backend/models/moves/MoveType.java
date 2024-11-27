package com.example.backend.models.moves;

public enum MoveType {
    NORMAL,
    ATTACK,
    EN_PASSANT,
    DOUBLE_PAWN_ADVANCE,
    PROMOTION,
    PROMOTION_ATTACK,
    KING_SIDE_CASTLE,
    QUEEN_SIDE_CASTLE;

    public boolean isCastleMove() {
        return this == KING_SIDE_CASTLE || this == QUEEN_SIDE_CASTLE;
    }

    public boolean isPromotion() {
        return this == PROMOTION || this == PROMOTION_ATTACK;
    }

    public boolean isKingSideCastle() {
        return this == KING_SIDE_CASTLE;
    }

    public boolean isAttack() {
        return this == ATTACK || this == PROMOTION_ATTACK;
    }
}
