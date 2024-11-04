package com.example.backend.models.moves;

public enum MoveType {
    NORMAL,
    ATTACK,
    EN_PASSANT,
    DOUBLE_PAWN_ADVANCE,
    PROMOTION,
    PROMOTION_ATTACK,
    KING_SIDE_CASTLE {
        @Override
        public boolean isCastleMove() {
            return true;
        }
    },
    QUEEN_SIDE_CASTLE {
        @Override
        public boolean isCastleMove() {
            return true;
        }
    };

    public boolean isCastleMove() {
        return false;
    }
}
