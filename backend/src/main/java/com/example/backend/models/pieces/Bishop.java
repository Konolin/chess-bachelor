package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;

public class Bishop extends Piece {
    public Bishop(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.BISHOP);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long occupancyBitBoard = board.getBitBoards().getAllPieces();
        long allMovesBitBoard = MagicBitBoards.getBishopAttacks(this.getPosition(), occupancyBitBoard);
        long friendlyPiecesBitBoard = board.getBitBoards().getAllianceBitBoard(this.getAlliance());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    @Override
    public Bishop movePiece(final Alliance alliance, final int toTilePosition) {
        return new Bishop(toTilePosition, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "B" : "b";
    }
}
