package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Rook piece in the chess game.
 * The Rook can move any number of squares along a row or column, but not diagonally.
 */
public class Rook extends Piece {

    /**
     * Constructs a Rook with the given position, alliance, and first move status.
     *
     * @param position    The current position of the Rook on the board.
     * @param alliance    The alliance (color) of the Rook (White or Black).
     * @param isFirstMove Whether this is the Rook's first move (used for castling).
     */
    public Rook(final int position, final Alliance alliance, final boolean isFirstMove) {
        super(position, alliance, isFirstMove, PieceType.ROOK);
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
     * Generates the legal moves for the Rook as a bitboard.
     * The Rook can move horizontally or vertically, so its legal moves are calculated
     * using the Rook's attack mask.
     *
     * @param board         The current state of the chess board.
     * @param piecePosition The current position of the Rook on the board.
     * @return A bitboard representing the legal move positions for the Rook.
     */
    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition) {
        // get the bitboard representing all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();
        // get the bitboard representing all possible moves for the Rook
        long allMovesBitBoard = MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(board.getMoveMaker());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the Rook to a new position on the board.
     *
     * @param alliance       The alliance (color) of the Rook (White or Black).
     * @param toTilePosition The new position to which the Rook is moved.
     * @return A new Rook instance at the specified position with the same alliance and no first move status.
     */
    @Override
    public Rook movePiece(final Alliance alliance, final int toTilePosition) {
        return new Rook(toTilePosition, alliance, false);
    }

    /**
     * Returns a string representation of the Rook piece.
     * The string is "R" for white and "r" for black.
     *
     * @return A string representing the Rook piece, either "R" or "r".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "R" : "r";
    }
}
