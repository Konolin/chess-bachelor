package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
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

    /**
     * Constructor to initialize the PiecesBitBoards based on the provided board configuration.
     * Sets the bitboard for each piece according to its position on the board.
     *
     * @param boardConfig A map of board positions to pieces.
     */
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
    }

    /**
     * Copy constructor to create a new PiecesBitBoards object with the same values as another.
     *
     * @param other The PiecesBitBoards object to copy.
     */
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

    /**
     * Updates the bitboards when a piece moves from one position to another.
     *
     * @param movingPiece The piece being moved.
     * @param fromIndex   The starting position of the piece.
     * @param toIndex     The target position for the piece.
     */
    public void updateMove(Piece movingPiece, int fromIndex, int toIndex) {
        long fromMask = ~(1L << fromIndex);
        long toMask = 1L << toIndex;

        updateBitBoardForPiece(movingPiece, fromMask, toMask);

        if (movingPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        allPieces = whitePieces | blackPieces;
    }

    /**
     * Updates the bitboards when a piece is captured.
     *
     * @param captureIndex     The position of the captured piece.
     * @param opponentAlliance The alliance of the opponent whose piece was captured.
     */
    public void updateCapture(int captureIndex, Alliance opponentAlliance) {
        long captureMask = ~(1L << captureIndex);
        // remove the captured piece from the appropriate bitboards
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

    /**
     * Updates the bitboards when a pawn is promoted to a new piece.
     *
     * @param pawn          The pawn that was promoted.
     * @param promotedPiece The new piece the pawn was promoted to.
     * @param fromPosition  The position of the pawn before promotion.
     * @param toPosition    The position of the new piece after promotion.
     */
    public void updatePromotion(final Piece pawn, final Piece promotedPiece, final int fromPosition, final int toPosition) {
        long fromMask = ~(1L << fromPosition);
        long toMask = 1L << toPosition;

        // remove the pawn from its bitboard
        if (pawn.getAlliance().isWhite()) {
            whitePawns &= fromMask;
            whitePieces &= fromMask;
        } else {
            blackPawns &= fromMask;
            blackPieces &= fromMask;
        }

        setBitBoardForPiece(promotedPiece, toPosition);

        // update the alliance-specific bitboards
        if (promotedPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        allPieces = whitePieces | blackPieces;
    }

    /**
     * Updates the bitboard for a specific piece when it moves.
     *
     * @param piece    The piece that is moving.
     * @param fromMask The mask to remove the piece from its old position.
     * @param toMask   The mask to add the piece to its new position.
     */
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

    /**
     * Sets the bitboard for a given piece at the specified position.
     *
     * @param piece    The piece to place on the board.
     * @param position The position on the board where the piece is located.
     */
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

    /**
     * Returns the bitboard for a specific piece type and alliance.
     *
     * @param pieceType The type of piece (PAWN, KNIGHT, etc.).
     * @param alliance  The alliance (white or black).
     * @return The bitboard for the specified piece type and alliance
     */
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

    /**
     * Returns the bitboard for all pieces of a specific alliance.
     *
     * @param alliance The alliance (white or black).
     * @return The bitboard for all pieces of the specified alliance.
     */
    public long getAllianceBitBoard(final Alliance alliance) {
        return alliance.isWhite() ? whitePieces : blackPieces;
    }
}
