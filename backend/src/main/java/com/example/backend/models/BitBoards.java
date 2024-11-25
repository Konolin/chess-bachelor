package com.example.backend.models;

import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Getter
@Setter
public class BitBoards {
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

    public BitBoards(final List<Piece> whitePieces, final List<Piece> blackPieces) {
        this.whitePawns = calculatePieceBitboard(whitePieces, PieceType.PAWN);
        this.blackPawns = calculatePieceBitboard(blackPieces, PieceType.PAWN);
        this.whiteRooks = calculatePieceBitboard(whitePieces, PieceType.ROOK);
        this.blackRooks = calculatePieceBitboard(blackPieces, PieceType.ROOK);
        this.whiteKnights = calculatePieceBitboard(whitePieces, PieceType.KNIGHT);
        this.blackKnights = calculatePieceBitboard(blackPieces, PieceType.KNIGHT);
        this.whiteBishops = calculatePieceBitboard(whitePieces, PieceType.BISHOP);
        this.blackBishops = calculatePieceBitboard(blackPieces, PieceType.BISHOP);
        this.whiteQueens = calculatePieceBitboard(whitePieces, PieceType.QUEEN);
        this.blackQueens = calculatePieceBitboard(blackPieces, PieceType.QUEEN);
        this.whiteKing = calculatePieceBitboard(whitePieces, PieceType.KING);
        this.blackKing = calculatePieceBitboard(blackPieces, PieceType.KING);

        // combine all pieces into a single occupied bitboard
        this.allPieces = whitePawns | blackPawns | whiteRooks | blackRooks |
                whiteKnights | blackKnights | whiteBishops | blackBishops |
                whiteQueens | blackQueens | whiteKing | blackKing;

        // combine white and black pieces into separate bitboards
        this.whitePieces = whitePawns | whiteRooks | whiteKnights | whiteBishops | whiteQueens | whiteKing;
        this.blackPieces = blackPawns | blackRooks | blackKnights | blackBishops | blackQueens | blackKing;

        logBitboards();
    }

    // helper method to calculate the bitboard for a specific piece type
    private long calculatePieceBitboard(List<Piece> pieces, PieceType pieceType) {
        long bitboard = 0L;
        for (Piece piece : pieces) {
            if (piece.getType() == pieceType) {
                bitboard |= 1L << piece.getPosition();
            }
        }
        return bitboard;
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
        return sb.toString();
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
