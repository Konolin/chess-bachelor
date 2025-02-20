package com.example.backend.models.pieces;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.models.board.Board;

/**
 * Represents a King piece in the chess game.
 * The King can move one square in any direction and has special rules for castling.
 */
public class King extends Piece {

    /**
     * Constructs a King with the given position, alliance, and first move status.
     *
     * @param position The current position of the King on the board.
     * @param alliance The alliance (color) of the King (White or Black).
     * @param isFirstMove Indicates whether this is the King's first move.
     */
    public King(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.KING);
    }

    /**
     * Generates the legal moves for the King as a bitboard.
     * The legal moves are calculated based on the King's attack mask and filtered
     * by removing squares occupied by friendly pieces.
     *
     * @param board The current state of the chess board.
     * @return A bitboard representing the legal move positions for the King.
     */
    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        // get all possible moves for the current position
        long legalMovesBitboard = BitBoardUtils.getKingAttackMask(this.getPosition());
        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(this.getAlliance());
        return legalMovesBitboard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the King to a new position on the board.
     *
     * @param alliance The alliance (color) of the King (White or Black).
     * @param toTilePosition The new position to which the King is moved.
     * @return A new King instance at the specified position with the same alliance and updated first move status.
     */
    @Override
    public King movePiece(final Alliance alliance, final int toTilePosition) {
        return new King(toTilePosition, alliance, false);
    }

    /**
     * Returns a string representation of the King piece.
     * The string is "K" for white and "k" for black.
     *
     * @return A string representing the King piece, either "K" or "k".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "K" : "k";
    }
}
