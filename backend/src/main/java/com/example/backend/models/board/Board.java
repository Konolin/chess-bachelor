package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Pawn;
import com.example.backend.models.pieces.Piece;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Board {
    private final Logger logger = LoggerFactory.getLogger(Board.class);

    private final List<Tile> tiles;
    private final Alliance moveMaker;

    private final List<Move> whiteLegalMoves;
    private final List<Move> blackLegalMoves;

    private final long whiteLegalMovesBitBoard;
    private final long blackLegalMovesBitBoard;

    private final List<Piece> whitePieces;
    private final List<Piece> blackPieces;
    private final Pawn enPassantPawn;
    private final PiecesBitBoards piecesBitBoards;
    // castle capabilities used for fen string generation
    private boolean isBlackKingSideCastleCapable;
    private boolean isBlackQueenSideCastleCapable;
    private boolean isWhiteKingSideCastleCapable;
    private boolean isWhiteQueenSideCastleCapable;

    private Board(Builder builder) {
        this.tiles = this.createTiles(builder);
        this.moveMaker = builder.moveMaker;

        this.whitePieces = calculatePieces(Alliance.WHITE);
        this.blackPieces = calculatePieces(Alliance.BLACK);
        this.enPassantPawn = builder.enPassantPawn;

        // initialize the BitBoards object
        this.piecesBitBoards = new PiecesBitBoards(builder.boardConfig);

        // calculate castle capabilities for both sides (used for fen string generation)
        calculateCastleCapabilities();

        this.whiteLegalMoves = calculateLegalMoves(Alliance.WHITE);
        this.blackLegalMoves = calculateLegalMoves(Alliance.BLACK);

        this.whiteLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.WHITE);
        this.blackLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.BLACK);

        this.whiteLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.WHITE));
        this.blackLegalMoves.addAll(CastleUtils.calculateCastleMoves(this, Alliance.BLACK));
    }

    private List<Tile> createTiles(final Builder builder) {
        final Tile[] tilesArray = new Tile[ChessUtils.TILES_NUMBER];
        for (int position = 0; position < ChessUtils.TILES_NUMBER; position++) {
            tilesArray[position] = Tile.createTile(builder.boardConfig.get(position), position);
        }
        return List.of(tilesArray);
    }

    public List<Move> calculateLegalMoves(final Alliance alliance) {
        List<Move> legalMoves = new ArrayList<>();
        List<Piece> pieces = alliance.isWhite() ? whitePieces : blackPieces;
        for (final Piece piece : pieces) {
            legalMoves.addAll(piece.generateLegalMovesList(this));
        }
        return legalMoves;
    }

    private List<Piece> calculatePieces(final Alliance alliance) {
        final List<Piece> pieces = new ArrayList<>(64);
        for (final Tile tile : tiles) {
            final Piece occupyingPiece = tile.getOccupyingPiece();
            if (tile.isOccupied() && occupyingPiece.getAlliance() == alliance) {
                pieces.add(occupyingPiece);
            }
        }
        return pieces;
    }


    private long calculateLegalMovesBitBoard(final Alliance alliance) {
        long attackingPositionsBitBoard = 0L;
        // add all the tiles that are attacked
        for (final Piece piece : getAlliancesPieces(alliance)) {
            if (piece.isPawn()) {
                long pawnBitboard = alliance.isWhite() ? piecesBitBoards.getWhitePawns() : piecesBitBoards.getBlackPawns();
                attackingPositionsBitBoard |= BitBoardUtils.calculatePawnAttackingBitboard(pawnBitboard, alliance);
            } else {
                attackingPositionsBitBoard |= piece.generateLegalMovesBitBoard(this);
            }
        }
        return attackingPositionsBitBoard;
    }

    private void calculateCastleCapabilities() {
        isBlackKingSideCastleCapable = false;
        isBlackQueenSideCastleCapable = false;
        isWhiteKingSideCastleCapable = false;
        isWhiteQueenSideCastleCapable = false;

        // calculate castle capabilities for white
        if (CastleUtils.calculateAlliancesKingEligibleForCastle(Alliance.WHITE, tiles)) {
            if (CastleUtils.calculateAlliancesRookEligibleForCastle(Alliance.WHITE, tiles, 3)) {
                isWhiteKingSideCastleCapable = true;
            }
            if (CastleUtils.calculateAlliancesRookEligibleForCastle(Alliance.WHITE, tiles, -4)) {
                isWhiteQueenSideCastleCapable = true;
            }
        }

        // calculate castle capabilities for black
        if (CastleUtils.calculateAlliancesKingEligibleForCastle(Alliance.BLACK, tiles)) {
            if (CastleUtils.calculateAlliancesRookEligibleForCastle(Alliance.BLACK, tiles, 3)) {
                isBlackKingSideCastleCapable = true;
            }
            if (CastleUtils.calculateAlliancesRookEligibleForCastle(Alliance.BLACK, tiles, -4)) {
                isBlackQueenSideCastleCapable = true;
            }
        }
    }

    public Board executeMove(final Move move) {
        // obtain the piece that is going to be moved
        final Piece movingPiece = getTileAtCoordinate(move.getFromTileIndex()).getOccupyingPiece();

        // prepare a new BitBoards instance to update the board state
        final PiecesBitBoards newPiecesBitBoards = new PiecesBitBoards(this.piecesBitBoards);

        // check if this is a promotion move
        if (move.getMoveType().isPromotion()) {
            // create the promoted piece on its new position
            final Piece promotedPiece = ChessUtils.createPieceFromCharAndPosition(move.getPromotedPieceChar(), move.getToTileIndex());

            // update the bitboards with the promoted piece
            newPiecesBitBoards.updatePromotion(movingPiece, promotedPiece, move.getFromTileIndex(), move.getToTileIndex());

            // build the new board state with the promoted piece
            return placePieces(new Board.Builder(), movingPiece)
                    .setPieceAtPosition(promotedPiece)
                    .setMoveMaker(moveMaker.getOpponent())
                    .build();
        }

        // regular move handling
        final Piece movedPiece = movingPiece.movePiece(movingPiece.getAlliance(), move.getToTileIndex());

        // update the bitboards for the move
        newPiecesBitBoards.updateMove(movingPiece, move.getFromTileIndex(), move.getToTileIndex());

        // Handle captures
        if (move.getMoveType().isAttack()) {
            newPiecesBitBoards.updateCapture(move.getToTileIndex(), moveMaker.getOpponent());
        }

        // initialize builder and place all pieces except the one being moved
        Board.Builder boardBuilder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(movedPiece);

        // handle special moves: en passant, double pawn advance, and castling
        handleEnPassant(move, boardBuilder, movedPiece, newPiecesBitBoards);
        CastleUtils.handleCastleMove(move, boardBuilder, moveMaker);

        // set the next move maker (switch turns) and return the new board state
        return boardBuilder
                .setMoveMaker(moveMaker.getOpponent())
                .build();
    }

    // helper method to handle en passant logic
    private void handleEnPassant(final Move move, Board.Builder boardBuilder, final Piece movedPiece, final PiecesBitBoards newPiecesBitBoards) {
        if (move.getMoveType() == MoveType.EN_PASSANT) {
            boardBuilder.setEmptyTile(enPassantPawn.getPosition());
            newPiecesBitBoards.updateCapture(enPassantPawn.getPosition(), moveMaker.getOpponent());
        }

        // set the en passant pawn if this move is a double pawn advance
        if (move.getMoveType() == MoveType.DOUBLE_PAWN_ADVANCE) {
            boardBuilder.setEnPassantPawn((Pawn) movedPiece);
        } else {
            boardBuilder.setEnPassantPawn(null);
        }
    }

    public Board.Builder placePieces(final Board.Builder builder, final Piece movedPiece) {
        for (final Piece piece : getAlliancesPieces(moveMaker)) {
            if (!movedPiece.equals(piece)) {
                builder.setPieceAtPosition(piece);
            }
        }
        for (final Piece piece : getAlliancesPieces(moveMaker.getOpponent())) {
            builder.setPieceAtPosition(piece);
        }
        return builder;
    }

    public List<Piece> getAlliancesPieces(final Alliance alliance) {
        return alliance.isWhite() ? whitePieces : blackPieces;
    }

    public Alliance getAllianceOfPieceAtPosition(final int position) {
        if (!ChessUtils.isValidPosition(position)) {
            throw new ChessException("Invalid position " + position, ChessExceptionCodes.INVALID_POSITION);
        }
        if (tiles.get(position).isEmpty()) {
            return null;
        }
        return tiles.get(position).getOccupyingPiece().getAlliance();
    }

    public boolean isAllianceInCheckMate(final Alliance alliance) {
        return isAllianceInCheck(alliance) && getAlliancesLegalMoves(alliance).isEmpty();
    }

    public boolean isAllianceInCheck(final Alliance alliance) {
        // find the position of the king for the given alliance
        int kingPosition = alliance.isWhite()
                ? BitBoardUtils.getLs1bIndex(piecesBitBoards.getWhiteKing())
                : BitBoardUtils.getLs1bIndex(piecesBitBoards.getBlackKing());

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

    public Tile getTileAtCoordinate(final int tileCoordinate) {
        return this.tiles.get(tileCoordinate);
    }

    public List<Move> getAlliancesLegalMoves(final Alliance alliance) {
        return alliance.isWhite()
                ? ChessUtils.filterMovesResultingInCheck(whiteLegalMoves, this)
                : ChessUtils.filterMovesResultingInCheck(blackLegalMoves, this);
    }

    public long getAlliancesLegalMovesBitBoard(final Alliance alliance) {
        return alliance.isWhite() ? whiteLegalMovesBitBoard : blackLegalMovesBitBoard;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            final String tileText = this.tiles.get(i).toString();
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
        private Pawn enPassantPawn;

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

        public Builder setEmptyTile(final int position) {
            this.boardConfig.put(position, null);
            return this;
        }

        public Builder setEnPassantPawn(Pawn enPassantPawn) {
            this.enPassantPawn = enPassantPawn;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}
