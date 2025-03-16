package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.MoveHistoryEntry;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
import com.example.backend.utils.MoveUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class Board {
    // a stack that contains all the moves made in the game
    // used to undo moves and restore the game state
    private final Deque<MoveHistoryEntry> moveHistory = new ArrayDeque<>();

    // an object that contains all the bitboards of the pieces from the board
    private final PiecesBitBoards piecesBBs;

    // bitBoards that contain all the positions that an alliance can attack
    private long whiteAttacksBB = 0L;
    private long blackAttacksBB = 0L;

    // a hashmap that contains the legal moves bitboards for each piece on the board
    // the key is the position of the piece, and the value is the bitboard of the legal moves for that piece
    private Map<Integer, Long> whiteLegalMovesBBs;
    private Map<Integer, Long> blackLegalMovesBBs;

    // the position of the en passant pawn (if none, it's -1)
    private int enPassantPawnPosition;

    // the alliance of the current player
    private Alliance moveMaker;

    // flags that indicate if the player can castle on the king side or queen side
    private boolean isBlackKingSideCastleCapable;
    private boolean isBlackQueenSideCastleCapable;
    private boolean isWhiteKingSideCastleCapable;
    private boolean isWhiteQueenSideCastleCapable;

    /**
     * Constructor for the Board class.
     * Initializes the board with the given configuration.
     *
     * @param builder the builder object that contains the board configuration
     */
    private Board(Builder builder) {
        this.moveMaker = builder.moveMaker;
        this.enPassantPawnPosition = builder.enPassantPawnPosition;

        // initialize the BitBoards object based on the board configuration
        this.piecesBBs = new PiecesBitBoards(builder.boardConfig);

        // initialize castle capabilities for both sides
        initCastleCapabilities(builder.castleCapabilities);

        // initialize the legal moves bitboard maps for both alliances
        this.whiteLegalMovesBBs = calculateLegalMovesBitBoards(Alliance.WHITE);
        this.blackLegalMovesBBs = calculateLegalMovesBitBoards(Alliance.BLACK);
    }

    /**
     * Converts a map of piece positions and move-destination bitboards into a list of Move objects.
     * This method iterates over the map entries, determines the piece type at each key position,
     * and then calls Piece.generateLegalMovesList to create Move objects for each set bit in the
     * destination bitboard. The resulting moves for all pieces are then collected into a single list.
     *
     * @param bitBoards a map where each key is the integer board coordinate of a piece, and each value
     *                  is a bitboard (long) indicating valid move destinations for that piece
     * @return a list of all Move objects representing the moves described by the bitboards
     */
    private MoveList convertBitBoardsToMoves(final Map<Integer, Long> bitBoards) {
        MoveList legalMoves = new MoveList();
        for (final Map.Entry<Integer, Long> entry : bitBoards.entrySet()) {
            legalMoves.addAll(Piece.generateLegalMovesList(this, entry.getKey(), moveMaker, getPieceTypeAtPosition(entry.getKey()), entry.getValue()));
        }
        return legalMoves;
    }

    /**
     * Calculates the legal moves bitboards for all pieces of the given alliance.
     * The method iterates over all the pieces of the given alliance, and for each piece,
     * it generates the legal moves bitboard using the Piece.generateLegalMovesBitBoard method.
     *
     * @param alliance the alliance for which to calculate the legal moves bitboards
     * @return a map where each key is the integer board coordinate of a piece, and each value
     * is a bitboard (long) indicating valid move destinations for that piece
     */
    private Map<Integer, Long> calculateLegalMovesBitBoards(final Alliance alliance) {
        Map<Integer, Long> legalMovesBitBoards = new HashMap<>();
        long[] alliancePiecesBitBoards = alliance.isWhite() ? piecesBBs.getWhiteBitboards() : piecesBBs.getBlackBitboards();

        // reset the attacks bitboard for the given alliance
        if (alliance.isWhite()) whiteAttacksBB = 0L;
        else blackAttacksBB = 0L;

        // iterate over all the piece types of the given alliance
        for (int i = 0; i < 6; i++) {
            PieceType type = ChessUtils.getPieceTypeByIndex(i);
            // get the bitboard of the pieces of the given alliance and type
            long allianceBitBoard = alliancePiecesBitBoards[i];

            // iterate over all the pieces of the given alliance and type
            while (allianceBitBoard != 0) {
                // isolate the lowest set bit
                long lsb = Long.lowestOneBit(allianceBitBoard);
                final int piecePosition = Long.numberOfTrailingZeros(allianceBitBoard);

                // generate the legal moves bitboard for the piece at the given position
                // and add it to the map and to the alliance legal moves bitboard
                final long legalMovesBitBoard = Piece.generateLegalMovesBitBoard(this, piecePosition, alliance, type);
                legalMovesBitBoards.put(piecePosition, legalMovesBitBoard);

                // update the attacks bitboard for the given alliance
                if (alliance.isWhite()) whiteAttacksBB |= legalMovesBitBoard;
                else blackAttacksBB |= legalMovesBitBoard;

                // clear that bit
                allianceBitBoard ^= lsb;
            }
        }

        return legalMovesBitBoards;
    }

    /**
     * Initializes the castle capabilities for both sides based on the given array.
     * The array contains 4 boolean values, in the following order:
     * [blackKingSideCastleCapable, blackQueenSideCastleCapable, whiteKingSideCastleCapable, whiteQueenSideCastleCapable]
     *
     * @param castleCapabilities the array containing the castle capabilities for both sides
     */
    private void initCastleCapabilities(final boolean[] castleCapabilities) {
        isBlackKingSideCastleCapable = castleCapabilities[0];
        isBlackQueenSideCastleCapable = castleCapabilities[1];
        isWhiteKingSideCastleCapable = castleCapabilities[2];
        isWhiteQueenSideCastleCapable = castleCapabilities[3];
    }

    /**
     * Updates the castle capabilities when a move is made.
     * The method checks if the moving piece is a king or a rook, or if a rook was captured,
     * and if it is, it updates the corresponding castle capability for the moving piece's alliance.
     *
     * @param movingPieceType   the type of the piece that moved
     * @param capturedPieceType the type of the piece that was captured
     * @param fromTileIndex     the index of the tile from which the piece moved
     * @param toTileIndex       the index of the tile to which the piece moved
     */
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
        this.isWhiteKingSideCastleCapable = moveHistoryEntry.isWhiteKingSideCastleCapable();
        this.isWhiteQueenSideCastleCapable = moveHistoryEntry.isWhiteQueenSideCastleCapable();
        this.isBlackKingSideCastleCapable = moveHistoryEntry.isBlackKingSideCastleCapable();
        this.isBlackQueenSideCastleCapable = moveHistoryEntry.isBlackQueenSideCastleCapable();
    }

    /**
     * Executes the given move on the board.
     * The method updates the piece bitboards, the en passant pawn position, the castle capabilities,
     * the legal moves bitboards, and the move maker. It also creates a MoveHistoryEntry object and
     * adds it to the move history stack.
     *
     * @param move the move to execute on the board
     */
    public void executeMove(final int move) {
        final int fromTileIndex = MoveUtils.getFromTileIndex(move);
        final int toTileIndex = MoveUtils.getToTileIndex(move);

        // get the type of pieces involved in this move
        PieceType movingPieceType = getPieceTypeAtPosition(fromTileIndex);
        PieceType capturedPieceType = getPieceTypeAtPosition(toTileIndex);

        // create the move history entry
        MoveHistoryEntry moveHistoryEntry = new MoveHistoryEntry(
                move,
                moveMaker,
                enPassantPawnPosition,
                capturedPieceType,
                movingPieceType,
                isWhiteKingSideCastleCapable,
                isWhiteQueenSideCastleCapable,
                isBlackKingSideCastleCapable,
                isBlackQueenSideCastleCapable,
                whiteLegalMovesBBs,
                blackLegalMovesBBs,
                whiteAttacksBB,
                blackAttacksBB
        );

        // handle enPassant move (set the captured piece type to PAWN)
        if (MoveUtils.getMoveType(move).isEnPassant()) {
            moveHistoryEntry.setCapturedPieceType(PieceType.PAWN);
        }

        // update the bitboards of all pieces to reflect the move
        piecesBBs.updateMove(move, movingPieceType, moveMaker);

        // update the enPassantPawnPosition if the move was a double pawn advance, else set it to -1
        enPassantPawnPosition = MoveUtils.getMoveType(move).isDoublePawnAdvance() ? toTileIndex : -1;

        // update castle capabilities
        updateCastleCapabilities(movingPieceType, capturedPieceType, fromTileIndex, toTileIndex);

        // update legalMovesBitBoards for both alliances
        // this will also update the attacks bitboards for both alliances
        whiteLegalMovesBBs = calculateLegalMovesBitBoards(Alliance.WHITE);
        blackLegalMovesBBs = calculateLegalMovesBitBoards(Alliance.BLACK);

        // change moveMaker
        moveMaker = moveMaker.getOpponent();

        // add the move to the move history
        moveHistory.push(moveHistoryEntry);
    }

    /**
     * Undoes the last move made on the board.
     * The method restores the previous enPassantPawnPosition, the piece bitboards, the castle capabilities,
     * the legal moves bitboards, and the move maker. It also pops the last MoveHistoryEntry from the move history stack.
     */
    public void undoLastMove() {
        // change moveMaker
        moveMaker = moveMaker.getOpponent();

        // get the last move history entry
        final MoveHistoryEntry moveHistoryEntry = moveHistory.pop();

        // restore the previous enPassantPawnPosition
        enPassantPawnPosition = moveHistoryEntry.getEnPassantPawnPosition();

        // restore pieceBitBoards
        piecesBBs.undoMove(moveHistoryEntry);

        // restore castle capabilities
        resetCastleCapabilities(moveHistoryEntry);

        // restore legalMovesBitBoards
        whiteLegalMovesBBs = moveHistoryEntry.getWhiteLegalMovesBitBoards();
        blackLegalMovesBBs = moveHistoryEntry.getBlackLegalMovesBitBoards();
        whiteAttacksBB = moveHistoryEntry.getWhiteLegalMovesBitBoard();
        blackAttacksBB = moveHistoryEntry.getBlackLegalMovesBitBoard();
    }

    /**
     * Checks if the given alliance is in stalemate.
     * An alliance is in stalemate if it is not in check, and it has no legal moves.
     *
     * @param alliance the alliance to check
     * @return true if the given alliance is in stalemate, false otherwise
     */
    public boolean isAllianceInStalemate(final Alliance alliance) {
        return !isAllianceInCheck(alliance) && getAlliancesLegalMovesBBs(alliance).isEmpty();
    }

    /**
     * Checks if the given alliance is in checkmate.
     * An alliance is in checkmate if it is in check, and it has no legal moves.
     *
     * @param alliance the alliance to check
     * @return true if the given alliance is in checkmate, false otherwise
     */
    public boolean isAllianceInCheckMate(final Alliance alliance) {
        return isAllianceInCheck(alliance) && getAlliancesLegalMovesBBs(alliance).isEmpty();
    }

    /**
     * Checks if the given alliance is in check.
     * An alliance is in check if the king is under attack.
     *
     * @param alliance the alliance to check
     * @return true if the given alliance is in check, false otherwise
     */
    public boolean isAllianceInCheck(final Alliance alliance) {
        // find the position of the king for the given alliance
        int kingPosition = alliance.isWhite()
                ? Long.numberOfTrailingZeros(piecesBBs.getWhiteBitboards()[BitBoardUtils.KING_INDEX])
                : Long.numberOfTrailingZeros(piecesBBs.getBlackBitboards()[BitBoardUtils.KING_INDEX]);

        // check if the king's position is attacked tiles of the opponent
        return (getAlliancesLegalMovesBB(alliance.getOpponent()) & (1L << kingPosition)) != 0;
    }

    /**
     * Checks if the tile at the given coordinate is occupied.
     *
     * @param tileCoordinate the coordinate of the tile to check
     * @return true if the tile is occupied, false otherwise
     */
    public boolean isTileOccupied(final int tileCoordinate) {
        return piecesBBs.getBitAtPosition(tileCoordinate) != 0;
    }

    /**
     * Returns the alliance of the piece at the given position.
     *
     * @param position the position of the piece
     * @return the alliance of the piece at the given position, or null if the tile is empty
     * @throws ChessException if the position is invalid, i.e. not in the range [0, 63]
     *                        with ChessExceptionCodes.INVALID_POSITION
     */
    public Alliance getPieceAllianceAtPosition(final int position) {
        if (!ChessUtils.isValidPosition(position)) {
            throw new ChessException("Invalid position " + position, ChessExceptionCodes.INVALID_POSITION);
        }
        if (!isTileOccupied(position)) {
            return null;
        }
        return piecesBBs.getAllianceOfTile(position);
    }

    /**
     * Returns the type of the piece at the given position.
     *
     * @param position the coordinate of the tile to check
     * @return the type of the piece at the given position, or null if the tile is empty
     */
    public PieceType getPieceTypeAtPosition(final int position) {
        return piecesBBs.getPieceTypeOfTile(position);
    }

    /**
     * Returns all the legal moves for the given alliance.
     * The methods converts the legal moves bitboards of the given alliance into a list of Move objects
     * using the convertBitBoardsToMoves method, it adds the castle moves
     * and then filters the moves that result in check.
     *
     * @param alliance alliance for which to get the legal moves
     * @return a list of all legal moves for the given alliance
     */
    public MoveList getAlliancesLegalMoves(final Alliance alliance) {
        MoveList legalMoves = new MoveList();
        legalMoves.addAll(convertBitBoardsToMoves(getAlliancesLegalMovesBBs(alliance)));
        legalMoves.addAll(CastleUtils.calculateCastleMoves(this, alliance));
        return ChessUtils.filterMovesResultingInCheck(legalMoves, piecesBBs, enPassantPawnPosition, alliance.getOpponent());
    }

    /**
     * Returns the legal moves bitboards for the given alliance.
     *
     * @param alliance the alliance for which to get the legal moves bitboards
     * @return a map where each key is the integer board coordinate of a piece, and each value
     * is a bitboard (long) indicating valid move destinations for that piece
     */
    public Map<Integer, Long> getAlliancesLegalMovesBBs(final Alliance alliance) {
        return alliance.isWhite() ? whiteLegalMovesBBs : blackLegalMovesBBs;
    }

    /**
     * Returns the bitboard with all the attacked tiles for the given alliance.
     *
     * @param alliance the alliance for which to get the legal moves bitboard
     * @return a bitboard (long) indicating all the attacked tiles for the given alliance
     */
    public long getAlliancesLegalMovesBB(final Alliance alliance) {
        return alliance.isWhite() ? whiteAttacksBB : blackAttacksBB;
    }

    /**
     * Returns algebraic notation for the piece at the given position.
     *
     * @param position the position of the piece
     * @return the algebraic notation for the piece at the given position
     */
    public String getPiecesAlgebraicNotationAtPosition(final int position) {
        // check if the tile is empty
        if (!isTileOccupied(position)) {
            return "-";
        }

        // get the piece type
        PieceType pieceType = getPieceTypeAtPosition(position);
        String pieceString = "";
        switch (pieceType) {
            case PAWN -> pieceString = "P";
            case KNIGHT -> pieceString = "N";
            case BISHOP -> pieceString = "B";
            case ROOK -> pieceString = "R";
            case QUEEN -> pieceString = "Q";
            case KING -> pieceString = "K";
        }

        return getPieceAllianceAtPosition(position).isWhite() ? pieceString : pieceString.toLowerCase();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            final String tileText = getPiecesAlgebraicNotationAtPosition(i);
            builder.append(String.format("%3s", tileText));
            if ((i + 1) % ChessUtils.TILES_PER_ROW == 0) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    @Setter
    public static class Builder {
        private final Map<Integer, Piece> boardConfig;
        private Alliance moveMaker;
        private int enPassantPawnPosition;
        private boolean[] castleCapabilities = new boolean[4];

        public Builder() {
            this.boardConfig = new HashMap<>();
        }

        public void setPieceAtPosition(final Piece piece) {
            this.boardConfig.put(piece.getPosition(), piece);
        }

        public Board build() {
            return new Board(this);
        }
    }
}
