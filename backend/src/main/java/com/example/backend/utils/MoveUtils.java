package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveDTO;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.PieceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The MoveUtils class provides utility methods for creating and extracting information from move integers.
 * A move integer is an 18-bit integer that encodes the source and destination tile indices of a move.
 * <ul>
 * <li> 6 bits (0-5) represent the source tile index </li>
 * <li> 6 bits (6-11) represent the destination tile index. </li>
 * <li> 3 bits (12-14) represent the promoted piece type </li>
 * <li> 3 bits (15-17) represent the move type </li>
 * </ul>
 */
public class MoveUtils {
    private MoveUtils() {
        throw new ChessException("Illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static int createMoveFromMoveDTO(MoveDTO moveDto) {
        return createMove(moveDto.getFromTileIndex(), moveDto.getToTileIndex(), moveDto.getMoveType(), moveDto.getPromotedPieceType());
    }

    public static MoveDTO createMoveDTOFromMove(int move) {
        return new MoveDTO(getFromTileIndex(move), getToTileIndex(move), getMoveType(move), getPromotedPieceType(move));
    }

    public static int createMove(int fromTileIndex, int toTileIndex, MoveType moveType, PieceType promotedPieceType) {
        int move = fromTileIndex | (toTileIndex << 6);
        move |= (promotedPieceType != null ? promotedPieceType.ordinal() : 0) << 12;
        move |= moveType.ordinal() << 15;
        return move;
    }

    public static int getFromTileIndex(int move) {
        return move & 0x3F;
    }

    public static int getToTileIndex(int move) {
        return (move >> 6) & 0x3F;
    }

    public static PieceType getPromotedPieceType(int move) {
        int promotedPieceType = (move >> 12) & 0x7;
        return switch (promotedPieceType) {
            case 0 -> null;
            case 1 -> PieceType.KNIGHT;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.ROOK;
            case 4 -> PieceType.QUEEN;
            default ->
                    throw new ChessException("Invalid promoted piece type in move int.", ChessExceptionCodes.INVALID_MOVE);
        };
    }

    public static MoveType getMoveType(int move) {
        int moveType = (move >> 15) & 0x7;
        return switch (moveType) {
            case 0 -> MoveType.NORMAL;
            case 1 -> MoveType.ATTACK;
            case 2 -> MoveType.EN_PASSANT;
            case 3 -> MoveType.DOUBLE_PAWN_ADVANCE;
            case 4 -> MoveType.PROMOTION;
            case 5 -> MoveType.PROMOTION_ATTACK;
            case 6 -> MoveType.KING_SIDE_CASTLE;
            case 7 -> MoveType.QUEEN_SIDE_CASTLE;
            default -> throw new ChessException("Invalid move type in move int.", ChessExceptionCodes.INVALID_MOVE);
        };
    }

    public static List<MoveDTO> createMoveDTOListFromMoveList(MoveList moveList) {
        if (moveList == null) {
            return Collections.emptyList();
        }
        List<MoveDTO> moveDTOList = new ArrayList<>();
        for (int i = 0; i < moveList.size(); i++) {
            moveDTOList.add(createMoveDTOFromMove(moveList.get(i)));
        }
        return moveDTOList;
    }

    /**
     * Provides a string representation of the move in the format:
     * <fromTileIndex> - <toTileIndex> (<moveType>)
     *
     * @param move The move integer to be converted to a string.
     * @return A string representing the move, including the tile indices and move type.
     */
    public static String toString(int move) {
        return getFromTileIndex(move) + " - " + getToTileIndex(move) + " ( " + getMoveType(move).name() + " ) ";
    }

    /**
     * Converts the move to algebraic notation, a human-readable format commonly used in chess.
     * The format depends on the type of move:
     * - For castling, it returns "O-O" or "O-O-O".
     * - For regular moves, it returns the starting and ending square in algebraic notation.
     * - For promotion moves, it appends "=<promotedPiece>" (e.g., "=Q" for promotion to a Queen).
     *
     * @param move The move integer to be converted to algebraic notation.
     * @return A string representing the move in algebraic notation.
     */
    public static String toAlgebraic(int move) {
        StringBuilder sb = new StringBuilder();

        if (getMoveType(move).isCastleMove()) {
            sb.append(getMoveType(move).isKingSideCastle() ? "O-O" : "O-O-O");
        } else {
            if (!getMoveType(move).isPromotion()) {
                sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(getFromTileIndex(move)));
            }
            sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(getToTileIndex(move)));

            if (getMoveType(move).isPromotion()) {
                sb.append("=").append(Objects.requireNonNull(getPromotedPieceType(move)).getAlgebraicSymbol());
            }
        }

        return sb.toString();
    }
}
