package com.example.backend.models.pieces;

import com.example.backend.models.bitboards.MagicBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.utils.BitBoardUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Piece {
    private final Alliance alliance;
    private final PieceType type;
    private final int position;

    public Piece(final int position, final Alliance alliance, final PieceType type) {
        this.position = position;
        this.alliance = alliance;
        this.type = type;
    }

    public static List<Move> generateLegalMovesList(final Board board, final int piecePosition, final Alliance alliance, final PieceType type) {
        List<Move> legalMoves = new ArrayList<>();

        // Generate the bitboard of all possible legal moves for this piece
        long legalMovesBitBoard = generateLegalMovesBitBoard(board, piecePosition, alliance, type);

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

    public static long generateLegalMovesBitBoard(final Board board, final int piecePosition, final Alliance alliance, final PieceType type) {
        // get the occupancy bitboard for all pieces on the board
        long occupancyBitBoard = board.getPiecesBitBoards().getAllPieces();

        // get all the moves of the specified piece
        long allMovesBitBoard = getAllMovesBitBoard(type, piecePosition, occupancyBitBoard);

        // filter out moves that are blocked by friendly pieces
        long friendlyPiecesBitBoard = board.getPiecesBitBoards().getAllianceBitBoard(alliance);
        return allMovesBitBoard & ~friendlyPiecesBitBoard;
    }

    private static long getAllMovesBitBoard(final PieceType type, final int piecePosition, final long occupancyBitBoard) {
        return switch (type) {
            case KING -> BitBoardUtils.getKingAttackMask(piecePosition);
            case KNIGHT -> BitBoardUtils.getKnightAttackMask(piecePosition);
            case BISHOP -> MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard);
            case ROOK -> MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
            case QUEEN -> MagicBitBoards.getBishopAttacks(piecePosition, occupancyBitBoard)
                    | MagicBitBoards.getRookAttacks(piecePosition, occupancyBitBoard);
            case PAWN -> 0;
        };
    }

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

    @Override
    public String toString() {
        return alliance.isWhite() ? type.getAlgebraicSymbol() : type.getAlgebraicSymbol().toLowerCase();
    }
}
