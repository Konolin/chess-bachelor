package com.example.backend.models.moves;

import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.ChessUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Move class represents a chess move from one tile to another.
 * It encapsulates information about the source and destination tiles, the type of move (e.g., regular move, castling, promotion),
 * and any special information related to the move (e.g., promoted piece type).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private int fromTileIndex;
    private int toTileIndex;
    private MoveType moveType;
    private PieceType promotedPieceType;

    /**
     * Constructor for creating a move with a specified from and to tile index, move type, and no promoted piece.
     *
     * @param fromTileIndex The index of the starting tile.
     * @param toTileIndex The index of the destination tile.
     * @param moveType The type of the move.
     */
    public Move(int fromTileIndex, int toTileIndex, MoveType moveType) {
        this.fromTileIndex = fromTileIndex;
        this.toTileIndex = toTileIndex;
        this.moveType = moveType;
        this.promotedPieceType = null;
    }

    /**
     * Provides a string representation of the move in the format:
     * <fromTileIndex> - <toTileIndex> ( <moveType> )
     *
     * @return A string representing the move, including the tile indices and move type.
     */
    @Override
    public String toString() {
        return fromTileIndex + " - " + toTileIndex + " ( " + moveType.name() + " ) ";
    }

    /**
     * Converts the move to algebraic notation, a human-readable format commonly used in chess.
     * The format depends on the type of move:
     * - For castling, it returns "O-O" or "O-O-O".
     * - For regular moves, it returns the starting and ending square in algebraic notation.
     * - For promotion moves, it appends "=<promotedPiece>" (e.g., "=Q" for promotion to a Queen).
     *
     * @return A string representing the move in algebraic notation.
     */
    public String toAlgebraic() {
        StringBuilder sb = new StringBuilder();

        if (moveType.isCastleMove()) {
            sb.append(moveType.isKingSideCastle() ? "O-O" : "O-O-O");
        } else {
            if (!moveType.isPromotion()) {
                sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(fromTileIndex));
            }
            sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(toTileIndex));

            if (moveType.isPromotion()) {
                sb.append("=").append(promotedPieceType.getAlgebraicSymbol());
            }
        }

        return sb.toString();
    }
}
