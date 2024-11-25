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
        if (!board.isAllianceCastleCapable(alliance)) {
            return castleMoves;
        }

        int kingPosition = alliance.isWhite() ? 60 : 4;
        int[] rookPositions = alliance.isWhite() ? new int[]{56, 63} : new int[]{0, 7};

        for (int rookPosition : rookPositions) {
            if (isRookEligibleForCastle(board, rookPosition, alliance)) {
                if (rookPosition < kingPosition && areTilesSafeForCastle(board, kingPosition, new int[]{-1, -2, -3})) {
                    castleMoves.add(new Move(kingPosition, kingPosition - 2, MoveType.QUEEN_SIDE_CASTLE));
                } else if (areTilesSafeForCastle(board, kingPosition, new int[]{1, 2})) {
                    castleMoves.add(new Move(kingPosition, kingPosition + 2, MoveType.KING_SIDE_CASTLE));
                }
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

    private static boolean areTilesSafeForCastle(Board board, int kingPosition, int[] offsets) {
        for (int offset : offsets) {
            Tile tile = board.getTileAtCoordinate(kingPosition + offset);
            if (tile.isOccupied() || board.getAlliancesAttackingPositions(board.getMoveMaker().getOpponent()).contains(tile.getPosition())) {
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
