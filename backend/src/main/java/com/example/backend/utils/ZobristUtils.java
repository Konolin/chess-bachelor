package com.example.backend.utils;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.PieceType;

import java.security.SecureRandom;
import java.util.Random;

public class ZobristUtils {
    private static final int NUM_PIECE_TYPES = 12;
    private static final int CASTLE_WHITE_KINGSIDE = 0;
    private static final int CASTLE_WHITE_QUEENSIDE = 1;
    private static final int CASTLE_BLACK_KINGSIDE = 2;
    private static final int CASTLE_BLACK_QUEENSIDE = 3;
    private static final int NUM_CASTLE_OPTIONS = 4;

    private static final long[][] PIECE_HASHES = new long[NUM_PIECE_TYPES][ChessUtils.TILES_NUMBER];
    private static final long[] CASTLING_HASHES = new long[NUM_CASTLE_OPTIONS];
    private static final long[] EN_PASSANT_HASHES = new long[ChessUtils.TILES_PER_ROW];
    private static final long SIDE_TO_MOVE_HASH;

    private ZobristUtils() {
        throw new ChessException("Illegal state", ChessExceptionCodes.ILLEGAL_STATE);
    }

    static {
        Random rand = new SecureRandom();

        for (int piece = 0; piece < NUM_PIECE_TYPES; piece++) {
            for (int square = 0; square < ChessUtils.TILES_NUMBER; square++) {
                PIECE_HASHES[piece][square] = rand.nextLong();
            }
        }

        for (int i = 0; i < NUM_CASTLE_OPTIONS; i++) {
            CASTLING_HASHES[i] = rand.nextLong();
        }

        for (int file = 0; file < ChessUtils.TILES_PER_ROW; file++) {
            EN_PASSANT_HASHES[file] = rand.nextLong();
        }

        SIDE_TO_MOVE_HASH = rand.nextLong();
    }

    public static long computeZobristHash(Board board) {
        long hash = 0L;

        for (int tile = 0; tile < 64; tile++) {
            PieceType pieceType = board.getPieceTypeOfTile(tile);
            if (pieceType == null) {
                continue;
            }
            int pieceIndex = pieceType.getIndex() + (board.getAllianceOfPieceAtPosition(tile).isWhite() ? 0 : 6);
            hash ^= PIECE_HASHES[pieceIndex][tile];
        }

        if (board.getMoveMaker().isWhite()) {
            hash ^= SIDE_TO_MOVE_HASH;
        }

        if (board.isWhiteKingSideCastleCapable()) {
            hash ^= CASTLING_HASHES[CASTLE_WHITE_KINGSIDE];
        }
        if (board.isWhiteQueenSideCastleCapable()) {
            hash ^= CASTLING_HASHES[CASTLE_WHITE_QUEENSIDE];
        }
        if (board.isBlackKingSideCastleCapable()) {
            hash ^= CASTLING_HASHES[CASTLE_BLACK_KINGSIDE];
        }
        if (board.isBlackQueenSideCastleCapable()) {
            hash ^= CASTLING_HASHES[CASTLE_BLACK_QUEENSIDE];
        }

        if (board.getEnPassantPawnPosition() != -1) {
            hash ^= EN_PASSANT_HASHES[board.getEnPassantPawnPosition() % 8];
        }

        return hash;
    }
}
