package com.example.backend.models.pieces;

import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.models.board.Board;

import java.util.ArrayList;
import java.util.List;

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
     * Generates a list of legal moves for the piece based on the current board state.
     * It calculates both normal moves and attack moves (moves to capture opponent pieces).
     *
     * @param board         The current state of the chess board.
     * @param piecePosition The position of the piece whose moves we are generating.
     * @return A list of legal moves available for this piece.
     */
    public static List<Move> generateLegalMovesList(final Board board, final int piecePosition) {
        List<Move> legalMoves = new ArrayList<>();

        // Generate the bitboard of all possible legal moves for this piece
        long legalMovesBitBoard = generateLegalMovesBitBoard(board, piecePosition);

        // Get the bitboard of the opponent's pieces
        long opponentPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(board.getMoveMaker().getOpponent());

        // Separate the attack moves (moves to opponent pieces) and normal moves (empty squares)
        long attackMoves = legalMovesBitBoard & opponentPiecesBitBoard;
        long normalMoves = legalMovesBitBoard & ~opponentPiecesBitBoard;

        // Convert the bitboards to Move objects and add them to the legal moves list
        legalMoves.addAll(bitBoardToMoveList(normalMoves, MoveType.NORMAL, piecePosition));
        legalMoves.addAll(bitBoardToMoveList(attackMoves, MoveType.ATTACK, piecePosition));

        return legalMoves;
    }

    /**
     * Generates the legal moves for the King as a bitboard.
     * The legal moves are calculated based on the King's attack mask and filtered
     * by removing squares occupied by friendly pieces.
     *
     * @param board The current state of the chess board.
     * @param piecePosition The current position of the King on the board.
     * @return A bitboard representing the legal move positions for the King.
     */
    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition) {
        // get all possible moves for the current position
        long legalMovesBitboard = BitBoardUtils.getKingAttackMask(piecePosition);
        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(board.getMoveMaker());
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
