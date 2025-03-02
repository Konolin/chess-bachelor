package com.example.backend.models.pieces;

import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic piece on the chessboard.
 * This class serves as the base for all specific piece types (e.g., King, Queen, Knight).
 * It provides common functionality for all pieces, such as generating legal moves and moving the piece.
 */
@Getter
public abstract class Piece {
    private final Alliance alliance;
    private final int cachedHashCode;
    private final PieceType type;
    @Setter
    private int position;
    @Setter
    private boolean isFirstMove;

    /**
     * Constructs a Piece with the given position, alliance, first move status, and type.
     *
     * @param position    The position of the piece on the board.
     * @param alliance    The alliance (color) of the piece.
     * @param isFirstMove Whether this is the piece's first move.
     * @param type        The type of the piece.
     */
    protected Piece(final int position, final Alliance alliance, final boolean isFirstMove, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.isFirstMove = isFirstMove;
        this.type = type;
        this.cachedHashCode = computeHashCode();
    }

    /**
     * Converts a bitboard of legal moves into a list of Move objects.
     *
     * @param bitBoard      The bitboard representing legal moves.
     * @param moveType      The type of move (normal or attack).
     * @param piecePosition The position of the piece making the move.
     * @return A list of Move objects representing the legal moves.
     */
    protected static List<Move> bitBoardToMoveList(long bitBoard, final MoveType moveType, final int piecePosition) {
        int bitCount = Long.bitCount(bitBoard);
        List<Move> legalMoves = new ArrayList<>(bitCount);
        while (bitBoard != 0) {
            int destination = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;
            legalMoves.add(new Move(piecePosition, destination, moveType));
        }
        return legalMoves;
    }

    /**
     * Checks whether this piece is equal to another object.
     * Two pieces are considered equal if they have the same position, alliance, and first move status.
     *
     * @param other The other object to compare.
     * @return true if the pieces are equal, false otherwise.
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Piece otherPiece)) {
            return false;
        }

        return position == otherPiece.getPosition() &&
                alliance == otherPiece.getAlliance() &&
                isFirstMove == otherPiece.isFirstMove();
    }

    /**
     * Returns the hash code for this piece.
     * The hash code is computed based on the position, alliance, and first move status.
     *
     * @return The hash code for this piece.
     */
    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

    /**
     * Computes the hash code for the piece.
     *
     * @return The computed hash code.
     */
    private int computeHashCode() {
        final int prime = 31;
        int result = alliance.hashCode();
        result = prime * result + position;
        result = prime * result + (isFirstMove ? 1 : 0);
        return result;
    }

    /**
     * Moves the piece to a new position on the board.
     *
     * @param alliance       The alliance (color) of the piece.
     * @param toTilePosition The new position to which the piece is moved.
     * @return A new instance of the piece at the specified position.
     */
    public abstract Piece movePiece(final Alliance alliance, final int toTilePosition);
}
