package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.BitBoardUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
@Setter
public class PiecesBitBoards {
    @JsonIgnore
    private final Logger logger = LoggerFactory.getLogger(PiecesBitBoards.class);

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

    public PiecesBitBoards(final Map<Integer, Piece> boardConfig) {
        for (Map.Entry<Integer, Piece> entry : boardConfig.entrySet()) {
            int position = entry.getKey();
            Piece piece = entry.getValue();
            if (piece != null) {
                // set the corresponding bitboard for the piece
                setBitBoardForPiece(piece, position);
            }
        }
        allPieces = whitePieces | blackPieces;
//        logBitBoards();
    }

    // copy constructor
    public PiecesBitBoards(final PiecesBitBoards other) {
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

    public void updateMove(Piece movingPiece, int fromIndex, int toIndex) {
        long fromMask = ~(1L << fromIndex);
        long toMask = 1L << toIndex;

        // Update the specific piece's bitboard
        updateBitBoardForPiece(movingPiece, fromMask, toMask);

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
        setBitBoardForPiece(promotedPiece, toPosition);

        // update alliance-specific bitboards
        if (promotedPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        allPieces = whitePieces | blackPieces;
    }

    private void updateBitBoardForPiece(final Piece piece, long fromMask, long toMask) {
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

    private void setBitBoardForPiece(Piece piece, int position) {
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

    public long getPieceBitBoard(final PieceType pieceType, final Alliance alliance) {
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

    public long getAllianceBitBoard(final Alliance alliance) {
        return alliance.isWhite() ? whitePieces : blackPieces;
    }

    private void logBitBoards() {
        logger.info("\nWhite pieces bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whitePieces));
        logger.info("\nBlack pieces bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackPieces));
        logger.info("\nAll pieces bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.allPieces));
        logger.info("\nWhite pawns bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whitePawns));
        logger.info("\nBlack pawns bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackPawns));
        logger.info("\nWhite rooks bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whiteRooks));
        logger.info("\nBlack rooks bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackRooks));
        logger.info("\nWhite knights bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whiteKnights));
        logger.info("\nBlack knights bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackKnights));
        logger.info("\nWhite bishops bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whiteBishops));
        logger.info("\nBlack bishops bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackBishops));
        logger.info("\nWhite queens bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whiteQueens));
        logger.info("\nBlack queens bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackQueens));
        logger.info("\nWhite king bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.whiteKing));
        logger.info("\nBlack king bitboard:\n{}", BitBoardUtils.bitBoardFormatedString(this.blackKing));
    }
}
