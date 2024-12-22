package com.example.backend.models.pieces;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.models.board.Board;

public class Knight extends Piece {

    public Knight(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.KNIGHT);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long legalMovesBitboard = BitBoardUtils.KNIGHT_ATTACK_MASK[this.getPosition()];
        long friendlyPiecesBitBoard = this.getAlliance().isWhite()
                ? board.getPiecesBitBoards().getWhitePieces()
                : board.getPiecesBitBoards().getBlackPieces();
        return legalMovesBitboard & ~friendlyPiecesBitBoard;
    }

    @Override
    public Knight movePiece(final Alliance alliance, final int toTilePosition) {
        return new Knight(toTilePosition, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "N" : "n";
    }
}
