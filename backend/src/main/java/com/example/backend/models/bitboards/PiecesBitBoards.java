package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Tile;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
                setBitBoardForPiece(piece.getType(), piece.getAlliance(), position);
            }
        }
    }

    /**
     * Constructor to initialize the PiecesBitBoards based on the provided list of tiles.
     * Sets the bitboard for each piece according to its position on the board.
     *
     * @param tiles A list of tiles on the board.
     */
    public PiecesBitBoards(final List<Tile> tiles) {
        for (final Tile tile : tiles) {
            int position = tile.getPosition();
            Piece piece = tile.getOccupyingPiece();
            if (piece != null) {
                // set the corresponding bitboard for the piece
                setBitBoardForPiece(piece.getType(), piece.getAlliance(), position);
            }
        }
        allPieces = whitePieces | blackPieces;
    }

    /**
     * Updates the bitboards when a piece moves from one position to another.
     * Removes the piece from its old position and adds it to its new position.
     * Updates the piece-specific, alliance-specific and the allPieces bitboard.
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
     * Removes the captured piece from the appropriate bitboards.
     * Updates the piece-specific, alliance-specific and the allPieces bitboard.
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
     * Removes the pawn from the appropriate bitboard and adds the newly promoted piece to their specific bitboard.
     *
     * @param position          The position of the tile where the promotion happened.
     * @param promotedPieceType The piece type the that pawn was promoted to.
     * @param alliance          The alliance of the pawn that was promoted.
     */
    public void updatePromotion(final int position, final PieceType promotedPieceType, final Alliance alliance) {
        long positionMask = 1L << position;

        // remove the pawn from its bitboard
        if (alliance.isWhite()) {
            whitePawns &= ~positionMask;
        } else {
            blackPawns &= ~positionMask;
        }

        setBitBoardForPiece(promotedPieceType, alliance, position);
    }

    /**
     * Updates the bitboards when a pawn is captured via en passant.
     * Removes the captured pawn from the appropriate bitboards.
     *
     * @param captureIndex     The position of the captured pawn.
     * @param opponentAlliance The alliance of the opponent whose pawn was captured.
     */
    public void updateEnPassant(int captureIndex, final Alliance opponentAlliance) {
        long captureMask = ~(1L << captureIndex);
        // remove the captured pawn from the appropriate bitboards
        if (opponentAlliance.isWhite()) {
            whitePieces &= captureMask;
            whitePawns &= captureMask;
        } else {
            blackPieces &= captureMask;
            blackPawns &= captureMask;
        }
        allPieces &= captureMask;
    }

    /**
     * Updates the piece-specific, alliance-specific and the allPieces bitboard.
     * Removes the rook from its old position and adds it to its new position.
     *
     * @param oldRookPosition The old position of the rook.
     * @param newRookPosition The new position of the rook.
     * @param alliance        The alliance of the rook that is castling.
     */
    public void updateCastling(int oldRookPosition, int newRookPosition, final Alliance alliance) {
        long newRookMask = 1L << newRookPosition;
        long oldRookMask = 1L << oldRookPosition;

        if (alliance.isWhite()) {
            whiteRooks = (whiteRooks & ~oldRookMask) | newRookMask;
            whitePieces = (whitePieces & ~oldRookMask) | newRookMask;
        } else {
            blackRooks = (blackRooks & ~oldRookMask) | newRookMask;
            blackPieces = (blackPieces & ~oldRookMask) | newRookMask;
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
     * Sets the bit corresponding to the piece's position in the appropriate bitboard.
     * Also sets the bit in the allPieces bitboard and alliance-specific bitboard.
     *
     * @param pieceType The type of piece (PAWN, KNIGHT, etc.).
     * @param alliance  The alliance (white or black).
     * @param position  The position on the board where the piece is located.
     */
    private void setBitBoardForPiece(final PieceType pieceType, final Alliance alliance, int position) {
        long positionMask = 1L << position;

        if (alliance.isWhite()) {
            whitePieces |= positionMask;
            switch (pieceType) {
                case PAWN -> whitePawns |= positionMask;
                case KNIGHT -> whiteKnights |= positionMask;
                case BISHOP -> whiteBishops |= positionMask;
                case ROOK -> whiteRooks |= positionMask;
                case QUEEN -> whiteQueens |= positionMask;
                case KING -> whiteKing |= positionMask;
            }
        } else {
            blackPieces |= positionMask;
            switch (pieceType) {
                case PAWN -> blackPawns |= positionMask;
                case KNIGHT -> blackKnights |= positionMask;
                case BISHOP -> blackBishops |= positionMask;
                case ROOK -> blackRooks |= positionMask;
                case QUEEN -> blackQueens |= positionMask;
                case KING -> blackKing |= positionMask;
            }
        }
        allPieces |= positionMask;
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
