package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;

/**
 * Represents a Queen piece in the chess game.
 * The Queen combines the movement abilities of both the Rook and the Bishop.
 */
public class Queen extends Piece {

    /**
     * Constructs a Queen with the given position and alliance.
     *
     * @param position The current position of the Queen on the board.
     * @param alliance The alliance (color) of the Queen (White or Black).
     */
    public Queen(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.QUEEN);
    }

    /**
     * Generates the legal moves for the Queen as a bitboard.
     * The Queen can move like both a Rook and a Bishop, so its legal moves are
     * calculated using the combination of the Rook's and Bishop's attack masks.
     *
     * @param board The current state of the chess board.
     * @return A bitboard representing the legal move positions for the Queen.
     */
    @Override
    public long generateLegalMovesBitBoard(final Board board) {
        // get the occupancy bitboard of all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();
        // get the bitboard of all possible moves for the Queen (Rook + Bishop)
        long allMovesBitBoard = MagicBitBoards.getBishopAttacks(this.getPosition(), occupancyBitBoard)
                | MagicBitBoards.getRookAttacks(this.getPosition(), occupancyBitBoard);
        // filter out the moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(this.getAlliance());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the Queen to a new position on the board.
     *
     * @param alliance The alliance (color) of the Queen (White or Black).
     * @param toTilePosition The new position to which the Queen is moved.
     * @return A new Queen instance at the specified position with the same alliance.
     */
    @Override
    public Queen movePiece(final Alliance alliance, final int toTilePosition) {
        return new Queen(toTilePosition, alliance);
    }

    /**
     * Returns a string representation of the Queen piece.
     * The string is "Q" for white and "q" for black.
     *
     * @return A string representing the Queen piece, either "Q" or "q".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "Q" : "q";
    }
}
