package com.example.backend.models.pieces;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.models.board.Board;

/**
 * Represents a Knight piece in the chess game.
 * The Knight has a unique movement pattern where it moves in an "L" shape.
 */
public class Knight extends Piece {

    /**
     * Constructs a Knight with the given position and alliance.
     *
     * @param position The current position of the Knight on the board.
     * @param alliance The alliance (color) of the Knight (White or Black).
     */
    public Knight(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.KNIGHT);
    }

    /**
     * Generates the legal moves for the Knight as a bitboard.
     * The legal moves are calculated based on the Knight's attack mask and filtered
     * by removing squares occupied by friendly pieces.
     *
     * @param board The current state of the chess board.
     * @return A bitboard representing the legal move positions for the Knight.
     */
    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        // get the attack mask for the Knight based on its current position
        long legalMovesBitboard = BitBoardUtils.getKnightAttackMask(this.getPosition());
        // filter out squares occupied by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(this.getAlliance());
        return legalMovesBitboard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the Knight to a new position on the board.
     *
     * @param alliance The alliance (color) of the Knight (White or Black).
     * @param toTilePosition The new position to which the Knight is moved.
     * @return A new Knight instance at the specified position with the same alliance.
     */
    @Override
    public Knight movePiece(final Alliance alliance, final int toTilePosition) {
        return new Knight(toTilePosition, alliance);
    }

    /**
     * Returns a string representation of the Knight piece.
     * The string is "N" for white and "n" for black.
     *
     * @return A string representing the Knight piece, either "N" or "n".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "N" : "n";
    }
}
