package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.models.pieces.Rook;

import java.util.ArrayList;
import java.util.List;

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
     * @param board   The current game board.
     * @param alliance The alliance (color) of the player requesting the castling move.
     * @return A list of valid castling moves for the specified alliance.
     */
    public static List<Move> calculateCastleMoves(Board board, Alliance alliance) {
        List<Move> castleMoves = new ArrayList<>();

        // check if castling is still possible for the given alliance
        if (!board.isAllianceCastleCapable(alliance)) {
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
            if (areTilesSafeForCastle(board, kingPosition, offsets, alliance)) {
                castleMoves.add(new Move(kingPosition, kingDestination, moveType));
            }
        }
        return castleMoves;
    }

    /**
     * Checks if the rook at the given position is eligible for castling.
     * A rook is eligible if it has not moved and belongs to the given alliance.
     *
     * @param board The current game board.
     * @param rookPosition The position of the rook to check.
     * @param alliance The alliance (color) of the player requesting the castling move.
     * @return true if the rook is eligible for castling, false otherwise.
     */
    private static boolean isRookEligibleForCastle(Board board, int rookPosition, Alliance alliance) {
        Tile tile = board.getTileAtCoordinate(rookPosition);
        return tile.isOccupied() &&
                tile.getOccupyingPiece().getType() == PieceType.ROOK &&
                tile.getOccupyingPiece().isFirstMove() &&
                tile.getOccupyingPiece().getAlliance() == alliance;
    }

    /**
     * Checks if the tiles between the king and rook are safe for castling.
     * A tile is safe if it is unoccupied and not under attack by the opponent.
     *
     * @param board The current game board.
     * @param kingPosition The current position of the king.
     * @param offsets The offsets representing the tiles between the king and rook.
     * @param alliance The alliance (color) of the player requesting the castling move.
     * @return true if the tiles between the king and rook are safe for castling, false otherwise.
     */
    private static boolean areTilesSafeForCastle(Board board, int kingPosition, int[] offsets, Alliance alliance) {
        for (int offset : offsets) {
            // check if tile is occupied
            if (board.getTileAtCoordinate(kingPosition + offset).isOccupied()) {
                return false;
            }
            // check if the tile is attacked by the opponent (offset -3 does not need to be checked for attacks)
            if (offset != -3 &&
                    (board.getAlliancesLegalMovesBitBoard(alliance.getOpponent()) & (1L << (kingPosition + offset))) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the king of the given alliance is eligible for castling.
     * The king is eligible if it has not moved and is present on the board.
     *
     * @param alliance The alliance (color) of the player.
     * @param tiles The list of tiles on the board.
     * @return true if the king is eligible for castling, false otherwise.
     */
    public static boolean calculateAlliancesKingEligibleForCastle(final Alliance alliance, final List<Tile> tiles) {
        final int position = alliance.isWhite() ? 60 : 4;
        return tiles.get(position).isOccupied() &&
                tiles.get(position).getOccupyingPiece().getType() == PieceType.KING &&
                tiles.get(position).getOccupyingPiece().isFirstMove();
    }

    /**
     * Checks if the rook of the given alliance, at the specified offset, is eligible for castling.
     * The rook is eligible if it has not moved and is present on the board.
     *
     * @param alliance The alliance (color) of the player.
     * @param tiles The list of tiles on the board.
     * @param offset The offset to determine the position of the rook (0 for king-side, 7 for queen-side).
     * @return true if the rook is eligible for castling, false otherwise.
     */
    public static boolean calculateAlliancesRookEligibleForCastle(final Alliance alliance, final List<Tile> tiles, final int offset) {
        final int position = alliance.isWhite() ? 60 + offset : 4 + offset;
        return tiles.get(position).isOccupied() &&
                tiles.get(position).getOccupyingPiece().getType() == PieceType.ROOK &&
                tiles.get(position).getOccupyingPiece().isFirstMove();
    }
}
