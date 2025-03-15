package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;

/**
 * Utility class for handling castling logic in chess.
 */
public class CastleUtils {
    private CastleUtils() {
        throw new ChessException("Illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    /**
     * Calculates all valid castling moves for the given alliance on the board.
     * Castling is only allowed if both the king and rook have not moved, and if
     * the squares between them are empty and not attacked by the opponent.
     *
     * @param board    The current game board.
     * @param alliance The alliance (color) of the player requesting the castling move.
     * @return A list of valid castling moves for the specified alliance.
     */
    public static MoveList calculateCastleMoves(Board board, Alliance alliance) {
        MoveList castleMoves = new MoveList();

        // check if castling is still possible for the given alliance
        if (!isAllianceCastleCapable(board, alliance)) {
            return castleMoves;
        }

        // determine the position of the king for the alliance
        int kingPosition = alliance.isWhite() ? 60 : 4;

        // check if the king is in check (if so, castling is not allowed)
        if ((board.getAlliancesLegalMovesBitBoard(alliance.getOpponent()) & (1L << kingPosition)) != 0) {
            return castleMoves;
        }

        // determine rook positions based on alliance (white or black)
        int[] rookPositions = alliance.isWhite() ? new int[]{56, 63} : new int[]{0, 7};
        for (int rookPosition : rookPositions) {
            if (!isRookEligibleForCastle(board, rookPosition, alliance)) {
                continue;
            }

            // determine the offsets for the king's move based on rook position (king-side or queen-side)
            int[] offsets = rookPosition < kingPosition ? new int[]{-1, -2, -3} : new int[]{1, 2};
            MoveType moveType = rookPosition < kingPosition ? MoveType.QUEEN_SIDE_CASTLE : MoveType.KING_SIDE_CASTLE;
            int kingDestination = rookPosition < kingPosition ? kingPosition - 2 : kingPosition + 2;

            // check if the tiles are safe for castling
            if (areTilesSafeForCastle(board, kingPosition, offsets)) {
                castleMoves.add(MoveUtils.createMove(kingPosition, kingDestination, moveType, null));
            }
        }
        return castleMoves;
    }

    /**
     * Checks if the alliance is still capable of castling.
     *
     * @param board    The current game board.
     * @param alliance The alliance (color) of the player requesting the castling move.
     * @return true if the alliance is still capable of castling, false otherwise.
     */
    private static boolean isAllianceCastleCapable(final Board board, final Alliance alliance) {
        if (alliance.isWhite()) {
            return board.isWhiteKingSideCastleCapable() || board.isWhiteQueenSideCastleCapable();
        }
        return board.isBlackKingSideCastleCapable() || board.isBlackQueenSideCastleCapable();
    }

    /**
     * Checks if the rook is eligible for castling.
     *
     * @param board        The current game board.
     * @param rookPosition The position of the rook to check.
     * @param alliance     The alliance (color) of the player requesting the castling move.
     * @return true if the rook is eligible for castling, false otherwise.
     */
    private static boolean isRookEligibleForCastle(Board board, int rookPosition, Alliance alliance) {
        if (alliance.isBlack()) {
            return rookPosition == 7 && board.isBlackKingSideCastleCapable() ||
                    rookPosition == 0 && board.isBlackQueenSideCastleCapable();
        } else {
            return rookPosition == 63 && board.isWhiteKingSideCastleCapable() ||
                    rookPosition == 56 && board.isWhiteQueenSideCastleCapable();
        }
    }

    /**
     * Checks if the tiles between the king and rook are safe for castling.
     * A tile is safe if it is unoccupied and not under attack by the opponent.
     *
     * @param board        The current game board.
     * @param kingPosition The current position of the king.
     * @param offsets      The offsets representing the tiles between the king and rook.
     * @return true if the tiles between the king and rook are safe for castling, false otherwise.
     */
    private static boolean areTilesSafeForCastle(Board board, int kingPosition, int[] offsets) {
        Alliance alliance = board.getMoveMaker();

        // calculate the bitboard of the opponent's legal moves (additionally, check the squares attacked by the opponent's pawns)
        long opponentPawnBitBoard = alliance.isWhite()
                ? board.getPiecesBBs().getBlackBitboards()[BitBoardUtils.PAWN_INDEX]
                : board.getPiecesBBs().getWhiteBitboards()[BitBoardUtils.PAWN_INDEX];
        long attackBitBoard = board.getAlliancesLegalMovesBitBoard(alliance.getOpponent()) |
                BitBoardUtils.calculatePawnAttackingBitboard(opponentPawnBitBoard, alliance.getOpponent());

        for (int offset : offsets) {
            // check if tile is occupied
            if (board.isTileOccupied(kingPosition + offset)) {
                return false;
            }
            // check if the tile is attacked by the opponent (offset -3 does not need to be checked for attacks)
            if (offset != -3 && (attackBitBoard & (1L << (kingPosition + offset))) != 0) {
                return false;
            }
        }
        return true;
    }
}
