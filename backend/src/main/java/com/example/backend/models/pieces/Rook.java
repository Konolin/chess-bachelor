package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.moves.Move;
import com.example.backend.models.board.Board;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {

    public Rook(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.ROOK);
    }

    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        long occupancyBitBoard = board.getBitBoards().getAllPieces();
        long allMovesBitBoard = MagicBitBoards.getRookAttacks(this.getPosition(), occupancyBitBoard);
        long friendlyPiecesBitBoard = board.getBitBoards().getAllianceBitBoard(this.getAlliance());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    @Override
    public Rook movePiece(final Alliance alliance, final int toTilePosition) {
        return new Rook(toTilePosition, alliance, false);
    }

    @Override
    public boolean isRook() {
        return true;
    }

    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
