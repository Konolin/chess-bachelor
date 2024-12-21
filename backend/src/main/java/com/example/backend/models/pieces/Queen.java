package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;

public class Queen extends Piece {
    public Queen(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.QUEEN);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();
        long allMovesBitBoard = MagicBitBoards.getBishopAttacks(this.getPosition(), occupancyBitBoard)
                | MagicBitBoards.getRookAttacks(this.getPosition(), occupancyBitBoard);
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(this.getAlliance());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    @Override
    public Queen movePiece(final Alliance alliance, final int toTilePosition) {
        return new Queen(toTilePosition, alliance);
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "Q" : "q";
    }
}
