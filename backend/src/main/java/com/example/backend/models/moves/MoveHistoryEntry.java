package com.example.backend.models.moves;

import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents a single move in the game history.
 * Contains all the necessary information to undo the move, this includes the move itself,
 * the moving piece type, the captured piece type, the en passant pawn position,
 * the move maker before the move, the castling capabilities before the move,
 * the map of legal moves bit boards for each piece and alliance before the move,
 * and the attacked tiles bitboards of each alliance before the move.
 */
@Getter
@AllArgsConstructor
public class MoveHistoryEntry {
    private final int move;
    private final Alliance moveMaker;
    private final int enPassantPawnPosition;

    @Setter
    private PieceType capturedPieceType;
    private final PieceType movingPieceType;

    private final boolean whiteKingSideCastleCapable;
    private final boolean whiteQueenSideCastleCapable;
    private final boolean blackKingSideCastleCapable;
    private final boolean blackQueenSideCastleCapable;

    private final Map<Integer, Long> whiteLegalMovesBitBoards;
    private final Map<Integer, Long> blackLegalMovesBitBoards;

    private final long whiteLegalMovesBitBoard;
    private final long blackLegalMovesBitBoard;
}
