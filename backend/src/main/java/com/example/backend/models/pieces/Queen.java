package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.utils.ChessUtils;
import com.example.backend.models.moves.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.MoveType;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {
    public Queen(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.QUEEN);
    }

    @Override
    public List<Move> generateLegalMoves(final Board board) {
        List<Move> legalMoves = new ArrayList<>();

        long occupancyBitBoard = board.getBitBoards().getAllPieces();
        long allMovesBitBoard = MagicBitBoards.getRookAttacks(this.getPosition(), occupancyBitBoard)
                | MagicBitBoards.getBishopAttacks(this.getPosition(), occupancyBitBoard);
        long opponentPiecesBitBoard = board.getBitBoards().getAllianceBitBoard(this.getAlliance().getOpponent());
        long friendlyPiecesBitBoard = board.getBitBoards().getAllianceBitBoard(this.getAlliance());

        long attackMoves = allMovesBitBoard & opponentPiecesBitBoard & ~friendlyPiecesBitBoard;
        long normalMoves = allMovesBitBoard & ~opponentPiecesBitBoard & ~friendlyPiecesBitBoard;

        while (attackMoves != 0) {
            int destination = Long.numberOfTrailingZeros(attackMoves);
            attackMoves &= attackMoves - 1;
            legalMoves.add(new Move(this.getPosition(), destination, MoveType.ATTACK));
        }

        while (normalMoves != 0) {
            int destination = Long.numberOfTrailingZeros(normalMoves);
            normalMoves &= normalMoves - 1;
            legalMoves.add(new Move(this.getPosition(), destination, MoveType.NORMAL));
        }

        return legalMoves;
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
