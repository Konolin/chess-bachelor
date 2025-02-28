package com.example.backend.models.bitboards;

import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveHistoryEntry;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
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

    // combined bitboards
    private long allPieces;
    private long whitePieces;
    private long blackPieces;

    // white piece bitboards
    private long whitePawns;
    private long whiteKnights;
    private long whiteBishops;
    private long whiteRooks;
    private long whiteQueens;
    private long whiteKing;

    // black piece bitboards
    private long blackPawns;
    private long blackKnights;
    private long blackBishops;
    private long blackRooks;
    private long blackQueens;
    private long blackKing;

    /**
     * Constructs all piece bitboards from a position-to-piece map.
     *
     * @param boardConfig A map of board positions to pieces.
     */
    public PiecesBitBoards(final Map<Integer, Piece> boardConfig) {
        boardConfig.forEach((position, piece) -> {
            if (piece != null) {
                addPieceToBitBoard(piece.getType(), piece.getAlliance(), position);
            }
        });
        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Updates bitboards after a move:
     * 1) Moves the piece from its old position to the new position.
     * 2) Handles captures, promotions, en passant, and castling.
     *
     * @param move        The move that was made.
     * @param movingPiece The piece that is moving.
     */
    public void updateMove(final Move move, final Piece movingPiece) {
        final long fromMask = ~(1L << move.getFromTileIndex());
        final long toMask = (1L << move.getToTileIndex());

        // move piece from old to new position in piece-specific bitboards
        updateBitBoardForPiece(movingPiece, fromMask, toMask);

        // update alliance-specific bitboards
        if (movingPiece.getAlliance().isWhite()) {
            whitePieces = (whitePieces & fromMask) | toMask;
        } else {
            blackPieces = (blackPieces & fromMask) | toMask;
        }
        updateAllPieces();

        // handle captures
        if (move.getMoveType().isAttack()) {
            removeCapturedPiece(move.getToTileIndex(), movingPiece.getAlliance().getOpponent());
        }

        // handle promotion
        if (move.getMoveType().isPromotion()) {
            promotePawn(move.getToTileIndex(), move.getPromotedPieceType(), movingPiece.getAlliance());
        }

        // handle en passant
        if (move.getMoveType().isEnPassant()) {
            final int captureIndex = move.getToTileIndex() + (movingPiece.getAlliance().isWhite() ? 8 : -8);
            removeCapturedPawnEnPassant(captureIndex, movingPiece.getAlliance().getOpponent());
        }

        // handle castling (move the rook as well)
        if (move.getMoveType().isCastleMove()) {
            final int oldRookPosition = move.getMoveType().isKingSideCastle() ? move.getFromTileIndex() + 3 : move.getFromTileIndex() - 4;
            final int newRookPosition = move.getMoveType().isKingSideCastle() ? move.getFromTileIndex() + 1 : move.getFromTileIndex() - 1;
            moveRookForCastling(oldRookPosition, newRookPosition, movingPiece.getAlliance());
        }
    }

    /**
     * Undoes the last move (stored in a MoveHistoryEntry).
     *
     * @param moveHistoryEntry The record of the last move, including moving/captured pieces.
     */
    public void undoMove(final MoveHistoryEntry moveHistoryEntry) {
        final Move move = moveHistoryEntry.getMove();
        final Piece movingPiece = moveHistoryEntry.getMovingPiece();
        final Piece capturedPiece = moveHistoryEntry.getCapturedPiece();

        final int fromTileIndex = move.getFromTileIndex();
        final int toTileIndex = move.getToTileIndex();

        // undo castling first: put the rook back where it was
        if (move.getMoveType().isCastleMove()) {
            final int oldRookPosition = move.getMoveType().isKingSideCastle() ? fromTileIndex + 3 : fromTileIndex - 4;
            final int newRookPosition = move.getMoveType().isKingSideCastle() ? fromTileIndex + 1 : fromTileIndex - 1;

            final long oldRookMask = 1L << oldRookPosition;
            final long newRookMask = 1L << newRookPosition;

            if (movingPiece.getAlliance().isWhite()) {
                whiteRooks &= ~newRookMask;
                whitePieces &= ~newRookMask;
                whiteRooks |= oldRookMask;
                whitePieces |= oldRookMask;
            } else {
                blackRooks &= ~newRookMask;
                blackPieces &= ~newRookMask;
                blackRooks |= oldRookMask;
                blackPieces |= oldRookMask;
            }
        }

        // undo promotion: remove the promoted piece, restore the pawn
        if (move.getMoveType().isPromotion()) {
            removePieceFromBitBoard(move.getPromotedPieceType(), movingPiece.getAlliance(), toTileIndex);
            addPieceToBitBoard(PieceType.PAWN, movingPiece.getAlliance(), fromTileIndex);
        }
        // normal or non-promotion move: remove piece from "to", add it back to "from"
        else {
            removePieceFromBitBoard(movingPiece.getType(), movingPiece.getAlliance(), toTileIndex);
            addPieceToBitBoard(movingPiece.getType(), movingPiece.getAlliance(), fromTileIndex);
        }

        // if something was captured (including en passant), restore it
        if (capturedPiece != null) {
            final int capturePosition = capturedPiece.getPosition();
            addPieceToBitBoard(capturedPiece.getType(), capturedPiece.getAlliance(), capturePosition);
        }

        // recalculate whitePieces / blackPieces, then allPieces
        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Returns the bitboard for a specific piece type and alliance.
     *
     * @param pieceType The type of piece (PAWN, KNIGHT, etc.).
     * @param alliance  The alliance (white or black).
     * @return The bitboard containing only the specified piece type for the given alliance.
     */
    public long getPieceBitBoard(final PieceType pieceType, final Alliance alliance) {
        return switch (pieceType) {
            case PAWN -> alliance.isWhite() ? whitePawns : blackPawns;
            case KNIGHT -> alliance.isWhite() ? whiteKnights : blackKnights;
            case BISHOP -> alliance.isWhite() ? whiteBishops : blackBishops;
            case ROOK -> alliance.isWhite() ? whiteRooks : blackRooks;
            case QUEEN -> alliance.isWhite() ? whiteQueens : blackQueens;
            case KING -> alliance.isWhite() ? whiteKing : blackKing;
        };
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


    /**
     * Removes a piece of the given type and alliance from the provided position in the bitboards.
     */
    private void removePieceFromBitBoard(final PieceType pieceType, final Alliance alliance, final int position) {
        final long positionMask = ~(1L << position);

        if (alliance.isWhite()) {
            switch (pieceType) {
                case PAWN -> whitePawns &= positionMask;
                case KNIGHT -> whiteKnights &= positionMask;
                case BISHOP -> whiteBishops &= positionMask;
                case ROOK -> whiteRooks &= positionMask;
                case QUEEN -> whiteQueens &= positionMask;
                case KING -> whiteKing &= positionMask;
            }
        } else {
            switch (pieceType) {
                case PAWN -> blackPawns &= positionMask;
                case KNIGHT -> blackKnights &= positionMask;
                case BISHOP -> blackBishops &= positionMask;
                case ROOK -> blackRooks &= positionMask;
                case QUEEN -> blackQueens &= positionMask;
                case KING -> blackKing &= positionMask;
            }
        }
    }

    /**
     * Sets the bit corresponding to the piece's position in the appropriate bitboard.
     * Also sets the bit in the alliance-specific bitboard and the allPieces bitboard.
     *
     * @param pieceType The type of piece (PAWN, KNIGHT, etc.).
     * @param alliance  The alliance of the piece (white or black).
     * @param position  The position on the board.
     */
    private void addPieceToBitBoard(final PieceType pieceType, final Alliance alliance, final int position) {
        final long positionMask = 1L << position;

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
     * Removes the captured piece from the appropriate bitboards.
     *
     * @param captureIndex     The position of the captured piece.
     * @param opponentAlliance Alliance of the captured piece.
     */
    private void removeCapturedPiece(final int captureIndex, final Alliance opponentAlliance) {
        final long captureMask = ~(1L << captureIndex);

        if (opponentAlliance.isWhite()) {
            whitePawns &= captureMask;
            whiteKnights &= captureMask;
            whiteBishops &= captureMask;
            whiteRooks &= captureMask;
            whiteQueens &= captureMask;
            whiteKing &= captureMask;
            whitePieces &= captureMask;
        } else {
            blackPawns &= captureMask;
            blackKnights &= captureMask;
            blackBishops &= captureMask;
            blackRooks &= captureMask;
            blackQueens &= captureMask;
            blackKing &= captureMask;
            blackPieces &= captureMask;
        }
        updateAllPieces();
    }

    /**
     * Promotes a pawn on the board by removing the pawn and adding the promoted piece.
     *
     * @param position          The board position of the promoted pawn.
     * @param promotedPieceType The new piece type (QUEEN, ROOK, BISHOP, or KNIGHT).
     * @param alliance          The alliance of the promoted pawn.
     */
    private void promotePawn(final int position, final PieceType promotedPieceType, final Alliance alliance) {
        final long positionMask = ~(1L << position);

        if (alliance.isWhite()) {
            whitePawns &= positionMask;  // remove the old pawn
        } else {
            blackPawns &= positionMask;
        }
        // add the promoted piece
        addPieceToBitBoard(promotedPieceType, alliance, position);

        updateAllianceBitBoards();
        updateAllPieces();
    }

    /**
     * Removes a pawn captured via en-passant.
     *
     * @param captureIndex     The position of the captured pawn.
     * @param opponentAlliance The alliance of the captured pawn.
     */
    private void removeCapturedPawnEnPassant(final int captureIndex, final Alliance opponentAlliance) {
        final long captureMask = ~(1L << captureIndex);

        if (opponentAlliance.isWhite()) {
            whitePawns &= captureMask;
            whitePieces &= captureMask;
        } else {
            blackPawns &= captureMask;
            blackPieces &= captureMask;
        }
        updateAllPieces();
    }

    /**
     * Moves the rook during castling.
     *
     * @param oldRookPosition The starting position of the rook.
     * @param newRookPosition The ending position of the rook.
     * @param alliance        The alliance of the rook.
     */
    private void moveRookForCastling(final int oldRookPosition, final int newRookPosition, final Alliance alliance) {
        final long oldMask = 1L << oldRookPosition;
        final long newMask = 1L << newRookPosition;

        if (alliance.isWhite()) {
            whiteRooks = (whiteRooks & ~oldMask) | newMask;
            whitePieces = (whitePieces & ~oldMask) | newMask;
        } else {
            blackRooks = (blackRooks & ~oldMask) | newMask;
            blackPieces = (blackPieces & ~oldMask) | newMask;
        }
        updateAllPieces();
    }

    /**
     * Updates the piece-specific bitboard when a piece moves from one tile to another.
     */
    private void updateBitBoardForPiece(final Piece piece, final long fromMask, final long toMask) {
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
     * Recalculates whitePieces and blackPieces
     * from the piece-specific white and black bitboards.
     */
    private void updateAllianceBitBoards() {
        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing;
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;
    }

    /**
     * Updates the allPieces bitboard as the union of white and black bitboards.
     */
    private void updateAllPieces() {
        allPieces = whitePieces | blackPieces;
    }
}
