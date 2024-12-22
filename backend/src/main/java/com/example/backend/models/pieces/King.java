package com.example.backend.models.pieces;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.models.board.Board;

public class King extends Piece {

    public King(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.KING);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long legalMovesBitboard = BitBoardUtils.KING_ATTACK_MASK[this.getPosition()];
        long friendlyPiecesBitBoard = this.getAlliance().isWhite()
                ? board.getPiecesBitBoards().getWhitePieces()
                : board.getPiecesBitBoards().getBlackPieces();
        return legalMovesBitboard & ~friendlyPiecesBitBoard;
    }

    @Override
    public King movePiece(final Alliance alliance, final int toTilePosition) {
        return new King(toTilePosition, alliance, false);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "K" : "k";
    }

    @Override
    public boolean isKing() {
        return true;
    }
}
