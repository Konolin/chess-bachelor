package com.example.backend.models.moves;

import com.example.backend.models.pieces.Piece;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single move in the game history.
 * Contains the move itself, the piece that moved, and the piece that was captured (if any).
 * Also includes a flag indicating if the moving piece is making its first move.
 * This information is used to undo moves and restore the game state.
 */
@Getter
public class MoveHistoryEntry {
    private final Move move;
    private final Piece movingPiece;
    @Setter
    private Piece capturedPiece;
    private final boolean isFirstMove;
    private final int enPassantPawnPosition;

    /**
     * Constructor for MoveHistoryEntry that initializes the move, moving piece, and captured piece.
     *
     * @param move          The move that was made.
     * @param movingPiece   The piece that moved.
     * @param capturedPiece The piece that was captured (null if no piece was captured).
     * @param isFirstMove   Flag indicating if the move was the moving piece's first move.
     */
    public MoveHistoryEntry(
            final Move move,
            final Piece movingPiece,
            final Piece capturedPiece,
            final boolean isFirstMove,
            final int enPassantPawnPosition){
        this.move = move;
        this.movingPiece = movingPiece;
        this.capturedPiece = capturedPiece;
        this.isFirstMove = isFirstMove;
        this.enPassantPawnPosition = enPassantPawnPosition;
    }

    @Override
    public String toString() {
        return "MoveHistoryEntry{" +
                "move=" + move +
                ", movingPiece=" + movingPiece +
                ", capturedPiece=" + capturedPiece +
                '}';
    }
}
