package com.example.backend.models.moves;

import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;
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
    private final PieceType movingPiece;
    private final int enPassantPawnPosition;
    @Setter
    private PieceType capturedPiece;
    private final Alliance moveMakerBefore;

    private final boolean whiteKingSideCastleCapableBefore;
    private final boolean whiteQueenSideCastleCapableBefore;
    private final boolean blackKingSideCastleCapableBefore;
    private final boolean blackQueenSideCastleCapableBefore;

    public MoveHistoryEntry(Move move,
                            PieceType movingPiece,
                            PieceType capturedPiece,
                            int enPassantPawnPosition,
                            Alliance moveMakerBefore,
                            boolean whiteKingSideBefore,
                            boolean whiteQueenSideBefore,
                            boolean blackKingSideBefore,
                            boolean blackQueenSideBefore) {
        this.move = move;
        this.movingPiece = movingPiece;
        this.capturedPiece = capturedPiece;
        this.enPassantPawnPosition = enPassantPawnPosition;
        this.moveMakerBefore = moveMakerBefore;
        this.whiteKingSideCastleCapableBefore = whiteKingSideBefore;
        this.whiteQueenSideCastleCapableBefore = whiteQueenSideBefore;
        this.blackKingSideCastleCapableBefore = blackKingSideBefore;
        this.blackQueenSideCastleCapableBefore = blackQueenSideBefore;
    }
}
