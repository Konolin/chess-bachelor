package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Rook;

import java.util.ArrayList;
import java.util.List;

public class CastleUtils {
    private CastleUtils() {
        throw new ChessException("Illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static List<Move> calculateCastleMoves(Board board, Alliance alliance) {
        List<Move> castleMoves = new ArrayList<>();

        // no need to check if fen tells us that the alliance is not capable of castling
        if (!board.isAllianceCastleCapable(alliance)) {
            return castleMoves;
        }

        int kingPosition = alliance.isWhite() ? 60 : 4;

        // check if king is not in check
        if ((board.getAlliancesAttackingPositionsBitBoard(alliance.getOpponent()) & (1L << kingPosition)) != 0) {
            return castleMoves;
        }

        int[] rookPositions = alliance.isWhite() ? new int[]{56, 63} : new int[]{0, 7};
        for (int rookPosition : rookPositions) {
            if (!isRookEligibleForCastle(board, rookPosition, alliance)) {
                continue;
            }

            int[] offsets = rookPosition < kingPosition ? new int[]{-1, -2, -3} : new int[]{1, 2};
            MoveType moveType = rookPosition < kingPosition ? MoveType.QUEEN_SIDE_CASTLE : MoveType.KING_SIDE_CASTLE;
            int kingDestination = rookPosition < kingPosition ? kingPosition - 2 : kingPosition + 2;

            if (areTilesSafeForCastle(board, kingPosition, offsets, alliance)) {
                castleMoves.add(new Move(kingPosition, kingDestination, moveType));
            }
        }
        return castleMoves;
    }

    private static boolean isRookEligibleForCastle(Board board, int rookPosition, Alliance alliance) {
        Tile tile = board.getTileAtCoordinate(rookPosition);
        return tile.isOccupied() &&
                tile.getOccupyingPiece().isRook() &&
                tile.getOccupyingPiece().isFirstMove() &&
                tile.getOccupyingPiece().getAlliance() == alliance;
    }

    private static boolean areTilesSafeForCastle(Board board, int kingPosition, int[] offsets, Alliance alliance) {
        for (int offset : offsets) {
            // check if tile is occupied
            if (board.getTileAtCoordinate(kingPosition + offset).isOccupied()) {
                return false;
            }
            // Check if the tile is attacked by the opponent (offset -3 does not need to be checked for attacks)
            if (offset != -3 &&
                    (board.getAlliancesAttackingPositionsBitBoard(alliance.getOpponent()) & (1L << (kingPosition + offset))) != 0) {
                return false;
            }

        }
        return true;
    }

    public static boolean calculateAlliancesKingEligibleForCastle(final Alliance alliance, final List<Tile> tiles) {
        final int position = alliance.isWhite() ? 60 : 4;
        return tiles.get(position).isOccupied() &&
                tiles.get(position).getOccupyingPiece().isKing() &&
                tiles.get(position).getOccupyingPiece().isFirstMove();
    }

    public static boolean calculateAlliancesRookEligibleForCastle(final Alliance alliance, final List<Tile> tiles, final int offset) {
        final int position = alliance.isWhite() ? 60 + offset : 4 + offset;
        return tiles.get(position).isOccupied() &&
                tiles.get(position).getOccupyingPiece().isRook() &&
                tiles.get(position).getOccupyingPiece().isFirstMove();
    }

    public static void handleCastleMove(final Move move, Board.Builder boardBuilder, final Alliance moveMaker) {
        if (move.getMoveType().isCastleMove()) {
            if (move.getMoveType() == MoveType.KING_SIDE_CASTLE) {
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() + 1, moveMaker, false))
                        .setEmptyTile(move.getFromTileIndex() + 3);
            } else { // Queen-side castle
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() - 1, moveMaker, false))
                        .setEmptyTile(move.getFromTileIndex() - 4);
            }
        }
    }
}
