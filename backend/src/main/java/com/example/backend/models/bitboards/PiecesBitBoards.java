package com.example.backend.models.bitboards;

import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveHistoryEntry;
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

/**
 * Maintains bitboards representing positions of all pieces on the chessboard.
 */
@Getter
@Setter
public class PiecesBitBoards {
    @JsonIgnore
    private final Logger logger = LoggerFactory.getLogger(PiecesBitBoards.class);

    // Combined bitboards
    private long allPieces;
    private long whitePieces;
    private long blackPieces;

    // White/Black piece bitboards as arrays (0 = Pawns, 1 = Knights, 2 = Bishops, 3 = Rooks, 4 = Queens, 5 = King)
    private long[] whiteBitboards = new long[6];
    private long[] blackBitboards = new long[6];

    /**
     * Constructs all piece bitboards from a position-to-piece map.
     *
     * @param boardConfig A map of board positions to pieces.
     */
    public PiecesBitBoards(final Map<Integer, Piece> boardConfig) {
        // Initialize everything to 0 just to be safe
        for (int i = 0; i < 6; i++) {
            whiteBitboards[i] = 0L;
            blackBitboards[i] = 0L;
        }

        boardConfig.forEach((position, piece) -> {
            if (piece != null) {
                addPieceToBitBoard(piece.getType(), piece.getAlliance(), position);
            }
        });
        // Fill in whitePieces/blackPieces and allPieces
        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Maps a PieceType to an index in our bitboard arrays.
     */
    private static int getPieceIndex(final PieceType pieceType) {
        return switch (pieceType) {
            case PAWN -> BitBoardUtils.PAWN_INDEX;
            case KNIGHT -> BitBoardUtils.KNIGHT_INDEX;
            case BISHOP -> BitBoardUtils.BISHOP_INDEX;
            case ROOK -> BitBoardUtils.ROOK_INDEX;
            case QUEEN -> BitBoardUtils.QUEEN_INDEX;
            case KING -> BitBoardUtils.KING_INDEX;
        };
    }

    /**
     * Updates bitboards after a move:
     * 1) Moves the piece from its old position to the new position.
     * 2) Handles captures, promotions, en passant, and castling.
     *
     * @param move            The move that was made.
     * @param movingPieceType The piece that is moving.
     * @param alliance        The alliance of the piece that is moving.
     */
    public void updateMove(final Move move, final PieceType movingPieceType, final Alliance alliance) {
        final long fromMask = ~(1L << move.getFromTileIndex());
        final long toMask = (1L << move.getToTileIndex());

        // 1) Move piece from old to new position in piece-specific bitboards
        updateBitBoardForPiece(movingPieceType, fromMask, toMask, alliance);

        // 2) Update alliance-specific bitboards
        if (alliance.isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        updateAllPieces();

        // 3) Handle captures
        if (move.getMoveType().isAttack()) {
            removeCapturedPiece(move.getToTileIndex(), alliance.getOpponent());
        }

        // 4) Handle promotion
        if (move.getMoveType().isPromotion()) {
            promotePawn(move.getToTileIndex(), move.getPromotedPieceType(), alliance);
        }

        // 5) Handle en passant
        if (move.getMoveType().isEnPassant()) {
            final int captureIndex = move.getToTileIndex() + (alliance.isWhite() ? 8 : -8);
            removeCapturedPawnEnPassant(captureIndex, alliance.getOpponent());
        }

        // 6) Handle castling (move the rook as well)
        if (move.getMoveType().isCastleMove()) {
            final int oldRookPosition = move.getMoveType().isKingSideCastle()
                    ? move.getFromTileIndex() + 3
                    : move.getFromTileIndex() - 4;
            final int newRookPosition = move.getMoveType().isKingSideCastle()
                    ? move.getFromTileIndex() + 1
                    : move.getFromTileIndex() - 1;
            moveRookForCastling(oldRookPosition, newRookPosition, alliance);
        }
    }

    /**
     * Undoes the last move (stored in a MoveHistoryEntry).
     *
     * @param moveHistoryEntry The record of the last move, including moving/captured pieces.
     */
    public void undoMove(final MoveHistoryEntry moveHistoryEntry) {
        final Move move = moveHistoryEntry.getMove();
        final PieceType movingPiece = moveHistoryEntry.getMovingPieceType();
        final PieceType captured = moveHistoryEntry.getCapturedPieceType();
        final Alliance moveMaker = moveHistoryEntry.getMoveMaker();

        final int fromTileIndex = move.getFromTileIndex();
        final int toTileIndex = move.getToTileIndex();

        // 1) Undo castling first: put the rook back where it was
        if (move.getMoveType().isCastleMove()) {
            final int oldRookPosition = move.getMoveType().isKingSideCastle()
                    ? fromTileIndex + 3
                    : fromTileIndex - 4;
            final int newRookPosition = move.getMoveType().isKingSideCastle()
                    ? fromTileIndex + 1
                    : fromTileIndex - 1;

            final long oldRookMask = 1L << oldRookPosition;
            final long newRookMask = 1L << newRookPosition;

            if (moveMaker.isWhite()) {
                // Remove rook from its new position, restore old
                whiteBitboards[BitBoardUtils.ROOK_INDEX] &= ~newRookMask;
                whitePieces &= ~newRookMask;
                whiteBitboards[BitBoardUtils.ROOK_INDEX] |= oldRookMask;
                whitePieces |= oldRookMask;
            } else {
                blackBitboards[BitBoardUtils.ROOK_INDEX] &= ~newRookMask;
                blackPieces &= ~newRookMask;
                blackBitboards[BitBoardUtils.ROOK_INDEX] |= oldRookMask;
                blackPieces |= oldRookMask;
            }
        }

        // 2) Undo promotion: remove the promoted piece, restore the pawn
        if (move.getMoveType().isPromotion()) {
            removePieceFromBitBoard(move.getPromotedPieceType(), moveMaker, toTileIndex);
            addPieceToBitBoard(PieceType.PAWN, moveMaker, fromTileIndex);
        }
        // For a non-promotion move: remove piece from "to", add it back to "from"
        else {
            removePieceFromBitBoard(movingPiece, moveMaker, toTileIndex);
            addPieceToBitBoard(movingPiece, moveMaker, fromTileIndex);
        }

        // 3) If something was captured (including en passant), restore it
        if (captured != null) {
            if (move.getMoveType().isEnPassant()) {
                addPieceToBitBoard(captured, moveMaker.getOpponent(), toTileIndex - 8 * moveMaker.getDirection());
            } else {
                addPieceToBitBoard(captured, moveMaker.getOpponent(), toTileIndex);
            }
        }

        // 4) Recalculate whitePieces / blackPieces, then allPieces
        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Returns the bitboard for a specific piece type and alliance.
     */
    public long getPieceBitBoard(final PieceType pieceType, final Alliance alliance) {
        int idx = getPieceIndex(pieceType);
        return alliance.isWhite()
                ? whiteBitboards[idx]
                : blackBitboards[idx];
    }

    /**
     * Returns the bitboard for all pieces of a specific alliance.
     */
    public long getAllianceBitBoard(final Alliance alliance) {
        return alliance.isWhite() ? whitePieces : blackPieces;
    }

    /**
     * Returns the bit at the specified position in the allPieces bitboard.
     */
    public long getBitAtPosition(final int position) {
        return (allPieces >> position) & 1L;
    }

    /**
     * Returns the alliance of the piece at the specified position, or null if empty.
     */
    public Alliance getAllianceOfTile(final int position) {
        if (((whitePieces >> position) & 1L) == 1) {
            return Alliance.WHITE;
        } else if (((blackPieces >> position) & 1L) == 1) {
            return Alliance.BLACK;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------------

    /**
     * Returns the type of the piece at the specified position, or null if empty.
     */
    public PieceType getPieceTypeOfTile(final int position) {
        long mask = 1L << position;

        // check all white piece bitboards
        for (int i = 0; i < PieceType.ALL_TYPES.length; i++) {
            if ((whiteBitboards[i] & mask) != 0) {
                return PieceType.ALL_TYPES[i];
            }
        }
        // check all black piece bitboards
        for (int i = 0; i < PieceType.ALL_TYPES.length; i++) {
            if ((blackBitboards[i] & mask) != 0) {
                return PieceType.ALL_TYPES[i];
            }
        }
        // if we find nothing, that means it's empty
        return null;
    }

    /**
     * Removes a piece of the given type/alliance from the given position.
     */
    private void removePieceFromBitBoard(final PieceType pieceType, final Alliance alliance, final int position) {
        long positionMask = ~(1L << position);
        int pieceIndex = getPieceIndex(pieceType);

        if (alliance.isWhite()) {
            whiteBitboards[pieceIndex] &= positionMask;
        } else {
            blackBitboards[pieceIndex] &= positionMask;
        }
    }

    /**
     * Adds a piece of the given type/alliance at the given position.
     */
    private void addPieceToBitBoard(final PieceType pieceType, final Alliance alliance, final int position) {
        long positionMask = (1L << position);
        int pieceIndex = getPieceIndex(pieceType);

        if (alliance.isWhite()) {
            whiteBitboards[pieceIndex] |= positionMask;
            whitePieces |= positionMask;
        } else {
            blackBitboards[pieceIndex] |= positionMask;
            blackPieces |= positionMask;
        }
        allPieces |= positionMask;
    }

    /**
     * Removes the captured piece from the appropriate bitboards.
     */
    private void removeCapturedPiece(final int captureIndex, final Alliance opponentAlliance) {
        long captureMask = ~(1L << captureIndex);

        if (opponentAlliance.isWhite()) {
            for (int i = 0; i < whiteBitboards.length; i++) {
                whiteBitboards[i] &= captureMask;
            }
            whitePieces &= captureMask;
        } else {
            for (int i = 0; i < blackBitboards.length; i++) {
                blackBitboards[i] &= captureMask;
            }
            blackPieces &= captureMask;
        }
        updateAllPieces();
    }

    /**
     * Promotes a pawn on the board by removing the pawn and adding the promoted piece.
     */
    private void promotePawn(final int position, final PieceType promotedPieceType, final Alliance alliance) {
        long positionMask = ~(1L << position);

        // Remove the old pawn
        if (alliance.isWhite()) {
            whiteBitboards[BitBoardUtils.PAWN_INDEX] &= positionMask;
        } else {
            blackBitboards[BitBoardUtils.PAWN_INDEX] &= positionMask;
        }

        // Add the promoted piece
        addPieceToBitBoard(promotedPieceType, alliance, position);

        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Removes a pawn captured via en passant.
     */
    private void removeCapturedPawnEnPassant(final int captureIndex, final Alliance opponentAlliance) {
        long captureMask = ~(1L << captureIndex);

        if (opponentAlliance.isWhite()) {
            whiteBitboards[BitBoardUtils.PAWN_INDEX] &= captureMask;
            whitePieces &= captureMask;
        } else {
            blackBitboards[BitBoardUtils.PAWN_INDEX] &= captureMask;
            blackPieces &= captureMask;
        }
        updateAllPieces();
    }

    /**
     * Moves the rook during castling.
     */
    private void moveRookForCastling(final int oldRookPosition, final int newRookPosition, final Alliance alliance) {
        long oldMask = (1L << oldRookPosition);
        long newMask = (1L << newRookPosition);

        if (alliance.isWhite()) {
            whiteBitboards[BitBoardUtils.ROOK_INDEX] = (whiteBitboards[BitBoardUtils.ROOK_INDEX] & ~oldMask) | newMask;
            whitePieces = (whitePieces & ~oldMask) | newMask;
        } else {
            blackBitboards[BitBoardUtils.ROOK_INDEX] = (blackBitboards[BitBoardUtils.ROOK_INDEX] & ~oldMask) | newMask;
            blackPieces = (blackPieces & ~oldMask) | newMask;
        }
        updateAllPieces();
    }

    /**
     * Updates the piece-specific bitboard when a piece moves from one tile to another.
     */
    private void updateBitBoardForPiece(final PieceType pieceType,
                                        final long fromMask,
                                        final long toMask,
                                        final Alliance alliance) {
        int pieceIndex = getPieceIndex(pieceType);

        if (alliance.isWhite()) {
            whiteBitboards[pieceIndex] =
                    (whiteBitboards[pieceIndex] & fromMask) | toMask;
        } else {
            blackBitboards[pieceIndex] =
                    (blackBitboards[pieceIndex] & fromMask) | toMask;
        }
    }

    /**
     * Recalculates whitePieces and blackPieces from the piece-specific bitboard arrays.
     */
    private void updateAllianceBitBoards() {
        whitePieces = 0L;
        blackPieces = 0L;

        for (int i = 0; i < 6; i++) {
            whitePieces |= whiteBitboards[i];
            blackPieces |= blackBitboards[i];
        }
    }

    /**
     * Updates the allPieces bitboard as the union of white and black bitboards.
     */
    private void updateAllPieces() {
        allPieces = whitePieces | blackPieces;
    }
}
