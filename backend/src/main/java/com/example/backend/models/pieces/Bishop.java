package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Bishop piece in the chess game.
 * A Bishop can move diagonally on the board, and this class provides the functionality to generate legal moves
 * based on the current board state and the Bishop's position.
 */
public class Bishop extends Piece {

    /**
     * Constructs a Bishop with the given position and alliance.
     *
     * @param position The current position of the Bishop on the board.
     * @param alliance The alliance (color) of the Bishop (White or Black).
     */
    public Bishop(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.BISHOP);
    }

    /**
     * Generates a list of legal moves for the piece based on the current board state.
     * It calculates both normal moves and attack moves (moves to capture opponent pieces).
     *
     * @param board         The current state of the chess board.
     * @param piecePosition The position of the piece whose moves we are generating.
     * @return A list of legal moves available for this piece.
     */
    public static List<Move> generateLegalMovesList(final Board board, final int piecePosition, final Alliance alliance) {
        List<Move> legalMoves = new ArrayList<>();

        // Generate the bitboard of all possible legal moves for this piece
        long legalMovesBitBoard = generateLegalMovesBitBoard(board, piecePosition, alliance);

        // Get the bitboard of the opponent's pieces
        long opponentPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(alliance.getOpponent());

        // Separate the attack moves (moves to opponent pieces) and normal moves (empty squares)
        long attackMoves = legalMovesBitBoard & opponentPiecesBitBoard;
        long normalMoves = legalMovesBitBoard & ~opponentPiecesBitBoard;

        // Convert the bitboards to Move objects and add them to the legal moves list
        legalMoves.addAll(bitBoardToMoveList(normalMoves, MoveType.NORMAL, piecePosition));
        legalMoves.addAll(bitBoardToMoveList(attackMoves, MoveType.ATTACK, piecePosition));

        return legalMoves;
    }

    /**
     * Generates the legal moves for the Bishop as a bitboard.
     * The legal moves are calculated by determining all possible attacks for the Bishop,
     * and then filtering out the squares occupied by friendly pieces.
     *
     * @param board         The current state of the chess board.
     * @param piecePosition The current position of the Bishop on the board.
     * @return A bitboard representing the legal move positions for the Bishop.
     */
    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition, final Alliance alliance) {
        // get the occupancy bitboard for all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();
        // get the bitboard of all possible attacks for this position and occupancy
        long allMovesBitBoard = MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard);
        // filter out the squares occupied by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(alliance);
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the Bishop to a new position on the board.
     *
     * @param alliance       The alliance (color) of the Bishop (White or Black).
     * @param toTilePosition The new position to which the Bishop is moved.
     * @return A new Bishop instance at the specified position with the same alliance.
     */
    @Override
    public Bishop movePiece(final Alliance alliance, final int toTilePosition) {
        return new Bishop(toTilePosition, alliance);
    }

    /**
     * Returns a string representation of the Bishop piece.
     * The string is "B" for white and "b" for black.
     *
     * @return A string representing the Bishop piece, either "B" or "b".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "B" : "b";
    }
}
