package com.example.backend.models;

public enum Alliance {
    WHITE {
        @Override
        public int getDirection() {
            return -1;
        }

        @Override
        public boolean isPawnPromotionRow(int candidatePosition) {
            return ChessUtils.EIGHTH_ROW[candidatePosition];
        }
    },
    BLACK {
        @Override
        public int getDirection() {
            return 1;
        }

        @Override
        public boolean isPawnPromotionRow(int candidatePosition) {
            return ChessUtils.FIRST_ROW[candidatePosition];
        }
    };

    public boolean isWhite() {
        return this == WHITE;
    }

    public boolean isBlack() {
        return this == BLACK;
    }

    public abstract int getDirection();
    public abstract boolean isPawnPromotionRow(int candidatePosition);
}
