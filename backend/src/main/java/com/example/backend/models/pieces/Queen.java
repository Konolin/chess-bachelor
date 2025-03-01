package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Queen piece in the chess game.
 * The Queen combines the movement abilities of both the Rook and the Bishop.
 */
public class Queen extends Piece {

    /**
     * Constructs a Queen with the given position and alliance.
     *
     * @param position The current position of the Queen on the board.
     * @param alliance The alliance (color) of the Queen (White or Black).
     */
    public Queen(final int position, final Alliance alliance) {
        super(position, alliance, false, PieceType.QUEEN);
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
     * Generates the legal moves for the Queen as a bitboard.
     * The Queen can move like both a Rook and a Bishop, so its legal moves are
     * calculated using the combination of the Rook's and Bishop's attack masks.
     *
     * @param board         The current state of the chess board.
     * @param piecePosition The current position of the Queen on the board.
     * @return A bitboard representing the legal move positions for the Queen.
     */
    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition) {
        // get the occupancy bitboard of all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();
        // get the bitboard of all possible moves for the Queen (Rook + Bishop)
        long allMovesBitBoard = MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard)
                | MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
        // filter out the moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(board.getMoveMaker());
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    /**
     * Moves the Queen to a new position on the board.
     *
     * @param alliance       The alliance (color) of the Queen (White or Black).
     * @param toTilePosition The new position to which the Queen is moved.
     * @return A new Queen instance at the specified position with the same alliance.
     */
    @Override
    public Queen movePiece(final Alliance alliance, final int toTilePosition) {
        return new Queen(toTilePosition, alliance);
    }

    /**
     * Returns a string representation of the Queen piece.
     * The string is "Q" for white and "q" for black.
     *
     * @return A string representing the Queen piece, either "Q" or "q".
     */
    @Override
    public String toString() {
        return this.getAlliance().isWhite() ? "Q" : "q";
    }
}
