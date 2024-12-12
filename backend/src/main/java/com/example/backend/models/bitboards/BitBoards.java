package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.ChessUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
@Setter
public class BitBoards {
    public static final long[] rookAttackMask = computeRookAttackMask();
    public static final long[] bishopAttackMask = computeBishopAttackMask();
    public static final long[] queenAttackMask = computeQueenAttackMask();

    @JsonIgnore
    private final Logger logger = LoggerFactory.getLogger(BitBoards.class);

    private long allPieces;
    private long whitePieces;
    private long blackPieces;
    private long whitePawns;
    private long whiteKnights;
    private long whiteBishops;
    private long whiteRooks;
    private long whiteQueens;
    private long whiteKing;
    private long blackPawns;
    private long blackKnights;
    private long blackBishops;
    private long blackRooks;
    private long blackQueens;
    private long blackKing;

    public BitBoards(final Map<Integer, Piece> boardConfig) {
        for (Map.Entry<Integer, Piece> entry : boardConfig.entrySet()) {
            int position = entry.getKey();
            Piece piece = entry.getValue();
            if (piece != null) {
                setBitboardForPiece(piece, position);
            }
        }
        allPieces = whitePieces | blackPieces;
//        logBitboards();
    }

    public BitBoards(final BitBoards other) {
        this.allPieces = other.allPieces;
        this.whitePieces = other.whitePieces;
        this.blackPieces = other.blackPieces;
        this.whitePawns = other.whitePawns;
        this.whiteKnights = other.whiteKnights;
        this.whiteBishops = other.whiteBishops;
        this.whiteRooks = other.whiteRooks;
        this.whiteQueens = other.whiteQueens;
        this.whiteKing = other.whiteKing;
        this.blackPawns = other.blackPawns;
        this.blackKnights = other.blackKnights;
        this.blackBishops = other.blackBishops;
        this.blackRooks = other.blackRooks;
        this.blackQueens = other.blackQueens;
        this.blackKing = other.blackKing;
    }

    /**
     * Computes the relevant occupancy masks for rook moves on a chessboard.
     * <p>
     * This method generates a lookup table where each entry represents the relevant
     * occupancy mask for a specific square on the board. The relevant occupancy mask
     * includes all squares in the same row and column as the rook, excluding edge squares
     * and the square where the rook is currently located.
     * <p>
     * For example:
     * - For a rook on `a1`, the mask will include `a2` to `a7` (vertical) and `b1` to `g1` (horizontal).
     * - For a rook on `d4`, the mask will include `d2` to `d7`, `b4` to `c4`, and `e4` to `g4`, excluding `d1` and `d8`.
     *
     * @return a long array of size 64, where each entry corresponds to the relevant
     *         occupancy mask for the rook at a specific square (indexed from 0 for `a1`
     *         to 63 for `h8`).
     */
    private static long[] computeRookAttackMask() {
        long[] table = new long[ChessUtils.TILES_NUMBER];

        // iterate over all squares of the board, creating a mask for every tile
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            long mask = 0L;
            int row = i / 8;
            int col = i % 8;

            // generate relevant occupancy for the row
            for (int j = col + 1; j < 7; j++) { // exclude edges
                mask |= 1L << (row * 8 + j);
            }
            for (int j = col - 1; j > 0; j--) { // exclude edges
                mask |= 1L << (row * 8 + j);
            }

            // Generate relevant occupancy for the column
            for (int j = row + 1; j < 7; j++) { // exclude edges
                mask |= 1L << (j * 8 + col);
            }
            for (int j = row - 1; j > 0; j--) { // exclude edges
                mask |= 1L << (j * 8 + col);
            }

            table[i] = mask;
        }

        return table;
    }

    /**
     * Computes the relevant occupancy masks for bishop moves on a chessboard.
     * <p>
     * This method generates a lookup table where each entry represents the relevant
     * occupancy mask for a specific square on the board. The relevant occupancy mask
     * includes all squares along the diagonals originating from the bishop's position,
     * excluding edge squares and the square where the bishop is currently located.
     * <p>
     * For example:
     * - For a bishop on `a1`, the mask will include `b2` to `g7` (top-right diagonal).
     * - For a bishop on `d4`, the mask will include squares such as `c3`, `b2`, `e5`, `f6`, etc.,
     *   excluding edge squares and those not part of the diagonals.
     *
     * @return a long array of size 64, where each entry corresponds to the relevant
     *         occupancy mask for the bishop at a specific square (indexed from 0 for `a1`
     *         to 63 for `h8`).
     */
    private static long[] computeBishopAttackMask() {
        long[] table = new long[ChessUtils.TILES_NUMBER];

        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            long mask = 0L;
            int row = i / 8;
            int col = i % 8;

            // Calculate relevant squares for the top-left to bottom-right diagonal
            for (int r = row + 1, c = col + 1; r < 7 && c < 7; r++, c++) {
                mask |= (1L << (r * 8 + c));
            }
            for (int r = row - 1, c = col - 1; r > 0 && c > 0; r--, c--) {
                mask |= (1L << (r * 8 + c));
            }

            // Calculate relevant squares for the top-right to bottom-left diagonal
            for (int r = row + 1, c = col - 1; r < 7 && c > 0; r++, c--) {
                mask |= (1L << (r * 8 + c));
            }
            for (int r = row - 1, c = col + 1; r > 0 && c < 7; r--, c++) {
                mask |= (1L << (r * 8 + c));
            }

            table[i] = mask;
        }

        return table;
    }

    private static long[] computeQueenAttackMask() {
        long[] table = new long[ChessUtils.TILES_NUMBER];
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            table[i] = rookAttackMask[i] | bishopAttackMask[i];
        }
        return table;
    }

    public void updateMove(Piece movingPiece, int fromIndex, int toIndex) {
        long fromMask = ~(1L << fromIndex);
        long toMask = 1L << toIndex;

        // Update the specific piece's bitboard
        updateBitboardForPiece(movingPiece, fromMask, toMask);

        // Update all pieces and alliance-specific bitboards
        if (movingPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        allPieces = whitePieces | blackPieces;
    }

    public void updateCapture(int captureIndex, Alliance opponentAlliance) {
        long captureMask = ~(1L << captureIndex);

        if (opponentAlliance.isWhite()) {
            whitePieces &= captureMask;
            whitePawns &= captureMask;
            whiteKnights &= captureMask;
            whiteBishops &= captureMask;
            whiteRooks &= captureMask;
            whiteQueens &= captureMask;
            whiteKing &= captureMask;
        } else {
            blackPieces &= captureMask;
            blackPawns &= captureMask;
            blackKnights &= captureMask;
            blackBishops &= captureMask;
            blackRooks &= captureMask;
            blackQueens &= captureMask;
            blackKing &= captureMask;
        }
    }

    public void updatePromotion(final Piece pawn, final Piece promotedPiece, final int fromPosition, final int toPosition) {
        // create a mask with the bit at the fromIndex to 0 and set the bit at the toIndex to 1
        long fromMask = ~(1L << fromPosition);
        long toMask = 1L << toPosition;

        // clear the pawn bitboard using the mask
        if (pawn.getAlliance().isWhite()) {
            whitePawns &= fromMask;
            whitePieces &= fromMask;
        } else {
            blackPawns &= fromMask;
            blackPieces &= fromMask;
        }

        // update the promoted piece's bitboard
        setBitboardForPiece(promotedPiece, toPosition);

        // update alliance-specific bitboards
        if (promotedPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        allPieces = whitePieces | blackPieces;
    }

    private void updateBitboardForPiece(final Piece piece, long fromMask, long toMask) {
        if (piece.getAlliance().isWhite()) {
            switch (piece.getType()) {
                case PAWN -> whitePawns = (whitePawns & fromMask) | toMask;
                case KNIGHT -> whiteKnights = (whiteKnights & fromMask) | toMask;
                case BISHOP -> whiteBishops = (whiteBishops & fromMask) | toMask;
                case ROOK -> whiteRooks = (whiteRooks & fromMask) | toMask;
                case QUEEN -> whiteQueens = (whiteQueens & fromMask) | toMask;
                case KING -> whiteKing = (whiteKing & fromMask) | toMask;
            }
        } else {
            switch (piece.getType()) {
                case PAWN -> blackPawns = (blackPawns & fromMask) | toMask;
                case KNIGHT -> blackKnights = (blackKnights & fromMask) | toMask;
                case BISHOP -> blackBishops = (blackBishops & fromMask) | toMask;
                case ROOK -> blackRooks = (blackRooks & fromMask) | toMask;
                case QUEEN -> blackQueens = (blackQueens & fromMask) | toMask;
                case KING -> blackKing = (blackKing & fromMask) | toMask;
            }
        }
    }

    private void setBitboardForPiece(Piece piece, int position) {
        long positionMask = 1L << position;

        if (piece.getAlliance().isWhite()) {
            whitePieces |= positionMask;
            switch (piece.getType()) {
                case PAWN -> whitePawns |= positionMask;
                case KNIGHT -> whiteKnights |= positionMask;
                case BISHOP -> whiteBishops |= positionMask;
                case ROOK -> whiteRooks |= positionMask;
                case QUEEN -> whiteQueens |= positionMask;
                case KING -> whiteKing |= positionMask;
            }
        } else {
            blackPieces |= positionMask;
            switch (piece.getType()) {
                case PAWN -> blackPawns |= positionMask;
                case KNIGHT -> blackKnights |= positionMask;
                case BISHOP -> blackBishops |= positionMask;
                case ROOK -> blackRooks |= positionMask;
                case QUEEN -> blackQueens |= positionMask;
                case KING -> blackKing |= positionMask;
            }
        }
    }

    public long getPieceBitboard(final PieceType pieceType, final Alliance alliance) {
        switch (pieceType) {
            case PAWN -> {
                return alliance.isWhite() ? whitePawns : blackPawns;
            }
            case KNIGHT -> {
                return alliance.isWhite() ? whiteKnights : blackKnights;
            }
            case BISHOP -> {
                return alliance.isWhite() ? whiteBishops : blackBishops;
            }
            case ROOK -> {
                return alliance.isWhite() ? whiteRooks : blackRooks;
            }
            case QUEEN -> {
                return alliance.isWhite() ? whiteQueens : blackQueens;
            }
            case KING -> {
                return alliance.isWhite() ? whiteKing : blackKing;
            }
            default -> throw new ChessException("Invalid piece type", ChessExceptionCodes.INVALID_PIECE_TYPE);
        }
    }

    public String bitboardFormatedString(long bitboard) {
        String bitboardString = String.format("%64s", Long.toBinaryString(bitboard)).replace(' ', '0');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bitboardString.length(); i++) {
            sb.append(bitboardString.charAt(i));
            if ((i + 1) % 8 == 0) {
                sb.append("\n");
            }
        }
        return sb.reverse().toString();
    }

    private void logBitboards() {
        logger.info("\nWhite pieces bitboard:\n{}", bitboardFormatedString(this.whitePieces));
        logger.info("\nBlack pieces bitboard:\n{}", bitboardFormatedString(this.blackPieces));
        logger.info("\nAll pieces bitboard:\n{}", bitboardFormatedString(this.allPieces));
        logger.info("\nWhite pawns bitboard:\n{}", bitboardFormatedString(this.whitePawns));
        logger.info("\nBlack pawns bitboard:\n{}", bitboardFormatedString(this.blackPawns));
        logger.info("\nWhite rooks bitboard:\n{}", bitboardFormatedString(this.whiteRooks));
        logger.info("\nBlack rooks bitboard:\n{}", bitboardFormatedString(this.blackRooks));
        logger.info("\nWhite knights bitboard:\n{}", bitboardFormatedString(this.whiteKnights));
        logger.info("\nBlack knights bitboard:\n{}", bitboardFormatedString(this.blackKnights));
        logger.info("\nWhite bishops bitboard:\n{}", bitboardFormatedString(this.whiteBishops));
        logger.info("\nBlack bishops bitboard:\n{}", bitboardFormatedString(this.blackBishops));
        logger.info("\nWhite queens bitboard:\n{}", bitboardFormatedString(this.whiteQueens));
        logger.info("\nBlack queens bitboard:\n{}", bitboardFormatedString(this.blackQueens));
        logger.info("\nWhite king bitboard:\n{}", bitboardFormatedString(this.whiteKing));
        logger.info("\nBlack king bitboard:\n{}", bitboardFormatedString(this.blackKing));
    }
}
