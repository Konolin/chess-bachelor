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
    private final PieceType type;
    @Setter
    private int position;

    /**
     * Constructs a Piece with the given position, alliance, first move status, and type.
     *
     * @param position    The position of the piece on the board.
     * @param alliance    The alliance (color) of the piece.
     * @param type        The type of the piece.
     */
    protected Piece(final int position, final Alliance alliance, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.type = type;
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
}
