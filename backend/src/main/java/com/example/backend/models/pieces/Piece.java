package com.example.backend.models.pieces;

import com.example.backend.models.moves.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic piece on the chessboard.
 * This class serves as the base for all specific piece types (e.g., King, Queen, Knight).
 * It provides common functionality for all pieces, such as generating legal moves and moving the piece.
 */
@Getter
public abstract class Piece {
    private final int position;
    private final Alliance alliance;
    private final boolean isFirstMove;
    private final int cachedHashCode;
    private final PieceType type;

    /**
     * Constructs a Piece with the given position, alliance, first move status, and type.
     *
     * @param position The position of the piece on the board.
     * @param alliance The alliance (color) of the piece.
     * @param isFirstMove Whether this is the piece's first move.
     * @param type The type of the piece.
     */
    protected Piece(final int position, final Alliance alliance, final boolean isFirstMove, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.isFirstMove = isFirstMove;
        this.type = type;
        this.cachedHashCode = computeHashCode();
    }

    /**
     * Generates a list of legal moves for the piece based on the current board state.
     * It calculates both normal moves and attack moves (moves to capture opponent pieces).
     *
     * @param board The current state of the chess board.
     * @return A list of legal moves available for this piece.
     */
    public List<Move> generateLegalMovesList(final Board board) {
        List<Move> legalMoves = new ArrayList<>();

        // Generate the bitboard of all possible legal moves for this piece
        long legalMovesBitBoard = generateLegalMovesBitBoard(board);

        // Get the bitboard of the opponent's pieces
        long opponentPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(this.getAlliance().getOpponent());

        // Separate the attack moves (moves to opponent pieces) and normal moves (empty squares)
        long attackMoves = legalMovesBitBoard & opponentPiecesBitBoard;
        long normalMoves = legalMovesBitBoard & ~opponentPiecesBitBoard;

        // Convert the bitboards to Move objects and add them to the legal moves list
        legalMoves.addAll(bitBoardToMoveList(normalMoves, MoveType.NORMAL));
        legalMoves.addAll(bitBoardToMoveList(attackMoves, MoveType.ATTACK));

        return legalMoves;
    }

    /**
     * Abstract method for generating a bitboard of legal moves for the piece.
     * The actual implementation is provided by subclasses representing specific pieces.
     *
     * @param board The current state of the chess board.
     * @return A bitboard representing the legal move positions for the piece.
     */
    public abstract long generateLegalMovesBitBoard(final Board board);

    /**
     * Converts a bitboard of legal moves into a list of Move objects.
     *
     * @param bitBoard The bitboard representing legal moves.
     * @param moveType The type of move (normal or attack).
     * @return A list of Move objects representing the legal moves.
     */
    protected List<Move> bitBoardToMoveList(long bitBoard, final MoveType moveType) {
        int bitCount = Long.bitCount(bitBoard);
        List<Move> legalMoves = new ArrayList<>(bitCount);
        while (bitBoard != 0) {
            int destination = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;
            legalMoves.add(new Move(this.getPosition(), destination, moveType));
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
     * @param alliance The alliance (color) of the piece.
     * @param toTilePosition The new position to which the piece is moved.
     * @return A new instance of the piece at the specified position.
     */
    public abstract Piece movePiece(final Alliance alliance, final int toTilePosition);
}
