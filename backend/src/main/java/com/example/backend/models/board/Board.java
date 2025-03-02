package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveHistoryEntry;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Getter
public class Board {
    private final Logger logger = LoggerFactory.getLogger(Board.class);

    private final Deque<MoveHistoryEntry> moveHistory = new ArrayDeque<>();

    private final PiecesBitBoards piecesBitBoards;
    private List<Move> whiteLegalMoves;
    private List<Move> blackLegalMoves;
    // now only used for checking if the king is in check and castle moves
    private long whiteLegalMovesBitBoard;
    private long blackLegalMovesBitBoard;
    private int enPassantPawnPosition;
    private Alliance moveMaker;
    // castle capabilities used for fen string generation
    private boolean isBlackKingSideCastleCapable;
    private boolean isBlackQueenSideCastleCapable;
    private boolean isWhiteKingSideCastleCapable;
    private boolean isWhiteQueenSideCastleCapable;

    private Board(Builder builder) {
        this.moveMaker = builder.moveMaker;
        this.enPassantPawnPosition = builder.enPassantPawnPosition;

        // initialize the BitBoards object
        this.piecesBitBoards = new PiecesBitBoards(builder.boardConfig);

        // calculate castle capabilities for both sides (used for fen string generation)
        initCastleCapabilities(builder.castleCapabilities);

        this.whiteLegalMoves = calculateAlliancesLegalMoves(Alliance.WHITE);
        this.blackLegalMoves = calculateAlliancesLegalMoves(Alliance.BLACK);

        this.whiteLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.WHITE);
        this.blackLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.BLACK);

        this.whiteLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.WHITE));
        this.blackLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.BLACK));
    }

    public List<Move> calculateAlliancesLegalMoves(final Alliance alliance) {
        List<Move> legalMoves = new ArrayList<>();
        long[] alliancePiecesBitBoards = alliance.isWhite() ? piecesBitBoards.getWhiteBitboards() : piecesBitBoards.getBlackBitboards();

        for (int i = 0; i < 6; i++) {
            PieceType type = ChessUtils.getPieceTypeByIndex(i);
            long allianceBitBoard = alliancePiecesBitBoards[i];

            while (allianceBitBoard != 0) {
                // isolate the lowest set bit
                long lsb = Long.lowestOneBit(allianceBitBoard);
                final int piecePosition = Long.numberOfTrailingZeros(allianceBitBoard);
                legalMoves.addAll(Piece.generateLegalMovesList(this, piecePosition, alliance, type));
                // clear that bit
                allianceBitBoard ^= lsb;
            }
        }

        return legalMoves;
    }

    private long calculateLegalMovesBitBoard(final Alliance alliance) {
        long legalMoves = 0L;
        long[] alliancePiecesBitBoards = alliance.isWhite() ? piecesBitBoards.getWhiteBitboards() : piecesBitBoards.getBlackBitboards();

        for (int i = 0; i < 6; i++) {
            PieceType type = ChessUtils.getPieceTypeByIndex(i);
            long allianceBitBoard = alliancePiecesBitBoards[i];

            while (allianceBitBoard != 0) {
                // isolate the lowest set bit
                long lsb = Long.lowestOneBit(allianceBitBoard);
                final int piecePosition = Long.numberOfTrailingZeros(allianceBitBoard);
                legalMoves |= Piece.generateLegalMovesBitBoard(this, piecePosition, alliance, type);
                // clear that bit
                allianceBitBoard ^= lsb;
            }
        }

        return legalMoves;
    }

    private void initCastleCapabilities(final boolean[] castleCapabilities) {
        isBlackKingSideCastleCapable = castleCapabilities[0];
        isBlackQueenSideCastleCapable = castleCapabilities[1];
        isWhiteKingSideCastleCapable = castleCapabilities[2];
        isWhiteQueenSideCastleCapable = castleCapabilities[3];
    }

    private void updateCastleCapabilities(final PieceType movingPieceType,
                                          final PieceType capturedPieceType,
                                          final int fromTileIndex,
                                          final int toTileIndex) {
        // remove castle capabilities if the king moves
        if (movingPieceType == PieceType.KING) {
            if (moveMaker.isWhite()) {
                isWhiteKingSideCastleCapable = false;
                isWhiteQueenSideCastleCapable = false;
            } else {
                isBlackKingSideCastleCapable = false;
                isBlackQueenSideCastleCapable = false;
            }
        }

        // remove castle capabilities if the rook moves
        if (movingPieceType == PieceType.ROOK) {
            if (moveMaker.isWhite()) {
                if (fromTileIndex == 63) {
                    isWhiteKingSideCastleCapable = false;
                } else if (fromTileIndex == 56) {
                    isWhiteQueenSideCastleCapable = false;
                }
            } else {
                if (fromTileIndex == 7) {
                    isBlackKingSideCastleCapable = false;
                } else if (fromTileIndex == 0) {
                    isBlackQueenSideCastleCapable = false;
                }
            }
        }

        // remove castle capabilities if rook is captured
        if (capturedPieceType == PieceType.ROOK) {
            if (moveMaker.getOpponent().isWhite()) {
                if (toTileIndex == 63) {
                    isWhiteKingSideCastleCapable = false;
                } else if (toTileIndex == 56) {
                    isWhiteQueenSideCastleCapable = false;
                }
            } else {
                if (toTileIndex == 7) {
                    isBlackKingSideCastleCapable = false;
                } else if (toTileIndex == 0) {
                    isBlackQueenSideCastleCapable = false;
                }
            }
        }
    }

    private void resetCastleCapabilities(MoveHistoryEntry moveHistoryEntry) {
        this.isWhiteKingSideCastleCapable = moveHistoryEntry.isWhiteKingSideCastleCapableBefore();
        this.isWhiteQueenSideCastleCapable = moveHistoryEntry.isWhiteQueenSideCastleCapableBefore();
        this.isBlackKingSideCastleCapable = moveHistoryEntry.isBlackKingSideCastleCapableBefore();
        this.isBlackQueenSideCastleCapable = moveHistoryEntry.isBlackQueenSideCastleCapableBefore();
    }

    public void executeMove(final Move move) {
        final int fromTileIndex = move.getFromTileIndex();
        final int toTileIndex = move.getToTileIndex();

        // get the pieces involved in this move
        PieceType movingPiece = getPieceTypeOfTile(fromTileIndex);
        PieceType capturedPiece = getPieceTypeOfTile(toTileIndex);

        // create the move history entry
        MoveHistoryEntry moveHistoryEntry = new MoveHistoryEntry(
                move,
                movingPiece,
                capturedPiece,
                enPassantPawnPosition,
                moveMaker,
                isWhiteKingSideCastleCapable,
                isWhiteQueenSideCastleCapable,
                isBlackKingSideCastleCapable,
                isBlackQueenSideCastleCapable
        );

        // handle enPassant move (remove captured enPassantPawn)
        if (move.getMoveType().isEnPassant()) {
            moveHistoryEntry.setCapturedPiece(getPieceTypeOfTile(enPassantPawnPosition));
        }

        // update the bitboard of the moving piece
        piecesBitBoards.updateMove(move, movingPiece, moveMaker);

        // update enPassantPawnPosition
        enPassantPawnPosition = move.getMoveType().isDoublePawnAdvance() ? toTileIndex : -1;

        // update castle capabilities
        updateCastleCapabilities(movingPiece, capturedPiece, fromTileIndex, toTileIndex);

        // update legal moves
        whiteLegalMoves = calculateAlliancesLegalMoves(Alliance.WHITE);
        blackLegalMoves = calculateAlliancesLegalMoves(Alliance.BLACK);

        // update legalMovesBitBoards
        whiteLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.WHITE);
        blackLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.BLACK);

        // change moveMaker
        moveMaker = moveMaker.getOpponent();

        // add the castle moves to the legal moves
        whiteLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.WHITE));
        blackLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.BLACK));

        // add the move to the move history
        moveHistory.push(moveHistoryEntry);
    }

    public void undoLastMove() {
        // change moveMaker
        moveMaker = moveMaker.getOpponent();

        // get the last move history entry
        final MoveHistoryEntry moveHistoryEntry = moveHistory.pop();

        // restore the previous enPassantPawnPosition
        enPassantPawnPosition = moveHistoryEntry.getEnPassantPawnPosition();

        // update pieceBitBoards
        piecesBitBoards.undoMove(moveHistoryEntry);

        // update castle capabilities
        resetCastleCapabilities(moveHistoryEntry);

        // update legal moves
        whiteLegalMoves = calculateAlliancesLegalMoves(Alliance.WHITE);
        blackLegalMoves = calculateAlliancesLegalMoves(Alliance.BLACK);

        // update legalMovesBitBoards
        whiteLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.WHITE);
        blackLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.BLACK);

        // add the castle moves to the legal moves
        whiteLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.WHITE));
        blackLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.BLACK));
    }

    public Alliance getAllianceOfPieceAtPosition(final int position) {
        if (!ChessUtils.isValidPosition(position)) {
            throw new ChessException("Invalid position " + position, ChessExceptionCodes.INVALID_POSITION);
        }
        if (!isTileOccupied(position)) {
            return null;
        }
        return getAllianceOfTile(position);
    }

    public boolean isAllianceInCheckMate(final Alliance alliance) {
        return isAllianceInCheck(alliance) && getAlliancesLegalMoves(alliance).isEmpty();
    }

    public boolean isAllianceInCheck(final Alliance alliance) {
        // find the position of the king for the given alliance
        int kingPosition = alliance.isWhite()
                ? Long.numberOfTrailingZeros(piecesBitBoards.getWhiteBitboards()[BitBoardUtils.KING_INDEX])
                : Long.numberOfTrailingZeros(piecesBitBoards.getBlackBitboards()[BitBoardUtils.KING_INDEX]);

        // get the attacking positions bitboard for the opponent
        long opponentAttackBitboard = getAlliancesLegalMovesBitBoard(alliance.getOpponent());

        // check if the king's position is attacked by the opponent
        return (opponentAttackBitboard & (1L << kingPosition)) != 0;
    }

    public boolean isAllianceCastleCapable(final Alliance alliance) {
        if (alliance.isWhite()) {
            return isWhiteKingSideCastleCapable || isWhiteQueenSideCastleCapable;
        }
        return isBlackKingSideCastleCapable || isBlackQueenSideCastleCapable;
    }

    public boolean isTileOccupied(final int tileCoordinate) {
        return piecesBitBoards.getBitAtPosition(tileCoordinate) != 0;
    }

    public Alliance getAllianceOfTile(final int tileCoordinate) {
        return piecesBitBoards.getAllianceOfTile(tileCoordinate);
    }

    public PieceType getPieceTypeOfTile(final int tileCoordinate) {
        return piecesBitBoards.getPieceTypeOfTile(tileCoordinate);
    }

    public String getPieceStringAtPosition(final int position) {
        // check if the tile is empty
        if (!isTileOccupied(position)) {
            return "-";
        }

        // get the piece type
        PieceType pieceType = getPieceTypeOfTile(position);
        String pieceString = "";
        switch (pieceType) {
            case PAWN -> pieceString = "P";
            case KNIGHT -> pieceString = "N";
            case BISHOP -> pieceString = "B";
            case ROOK -> pieceString = "R";
            case QUEEN -> pieceString = "Q";
            case KING -> pieceString = "K";
        }

        return getAllianceOfTile(position).isWhite() ? pieceString : pieceString.toLowerCase();
    }

    public List<Move> getAlliancesLegalMoves(final Alliance alliance) {
        return alliance.isWhite()
                ? ChessUtils.filterMovesResultingInCheck(
                whiteLegalMoves, piecesBitBoards, enPassantPawnPosition, moveMaker.getOpponent())
                : ChessUtils.filterMovesResultingInCheck(
                blackLegalMoves, piecesBitBoards, enPassantPawnPosition, moveMaker.getOpponent());
    }

    public long getAlliancesLegalMovesBitBoard(final Alliance alliance) {
        return alliance.isWhite() ? whiteLegalMovesBitBoard : blackLegalMovesBitBoard;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            final String tileText = getPieceStringAtPosition(i);
            builder.append(String.format("%3s", tileText));
            if ((i + 1) % ChessUtils.TILES_PER_ROW == 0) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public static class Builder {
        private final Map<Integer, Piece> boardConfig;
        private Alliance moveMaker;
        private int enPassantPawnPosition;
        private boolean[] castleCapabilities = new boolean[4];

        public Builder() {
            this.boardConfig = new HashMap<>();
        }

        public Builder setMoveMaker(final Alliance moveMaker) {
            this.moveMaker = moveMaker;
            return this;
        }

        public Builder setPieceAtPosition(final Piece piece) {
            this.boardConfig.put(piece.getPosition(), piece);
            return this;
        }

        public Builder setEnPassantPawnPosition(int enPassantPawnPosition) {
            this.enPassantPawnPosition = enPassantPawnPosition;
            return this;
        }

        public Builder setCastleCapabilities(boolean[] castleCapabilities) {
            this.castleCapabilities = castleCapabilities;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}
