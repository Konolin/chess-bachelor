package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.*;
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
    private final List<Integer> whiteAttackingPositions;
    private final List<Integer> blackAttackingPositions;

    private final List<Piece> whitePieces;
    private final List<Piece> blackPieces;
    private final Pawn enPassantPawn;

    // castle capabilities used for fen string generation
    private boolean isBlackKingSideCastleCapable;
    private boolean isBlackQueenSideCastleCapable;
    private boolean isWhiteKingSideCastleCapable;
    private boolean isWhiteQueenSideCastleCapable;

    private PiecesBitBoards piecesBitBoards;

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

        this.whiteAttackingPositions = calculateAttackingPositions(Alliance.WHITE);
        this.blackAttackingPositions = calculateAttackingPositions(Alliance.BLACK);

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
        return tiles.stream()
                .filter(Tile::isOccupied)
                .map(Tile::getOccupyingPiece)
                .filter(piece -> piece.getAlliance() == alliance)
                .toList();
    }

    private List<Integer> calculateAttackingPositions(final Alliance alliance) {
        List<Integer> attackingPositions = new ArrayList<>();

        // calculate pawn attacks
        for (final Piece piece : (alliance.isWhite() ? whitePieces : blackPieces)) {
            if (piece.isPawn()) {
                attackingPositions.addAll(calculatePawnAttackingPositions(piece, alliance));
            }
        }

        // calculate legal move attacks for all pieces
        for (final Move move : (alliance.isWhite() ? whiteLegalMoves : blackLegalMoves)) {
            attackingPositions.add(move.getToTileIndex());
        }

        return attackingPositions.stream().distinct().toList();
    }

    private List<Integer> calculatePawnAttackingPositions(Piece pawn, Alliance alliance) {
        List<Integer> attacks = new ArrayList<>();

        int pos = pawn.getPosition();
        int attack1 = alliance.isWhite() ? pos - 7 : pos + 7;
        int attack2 = alliance.isWhite() ? pos - 9 : pos + 9;
        int column = pos % 8;

        // check attack1 (right diagonal for white, left diagonal for black)
        if (ChessUtils.isValidPosition(attack1) && column != 7) {
            attacks.add(attack1);
        }

        // check attack2 (left diagonal for white, right diagonal for black)
        if (ChessUtils.isValidPosition(attack2) && column != 0) {
            attacks.add(attack2);
        }

        return attacks;
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

    public boolean isAllianceInCheck(final Alliance alliance) {
        return getAlliancesPieces(alliance)
                .stream()
                .filter(Piece::isKing)
                .findFirst()
                .map(Piece::getPosition)
                .map(position -> getAlliancesAttackingPositions(alliance.getOpponent()).contains(position))
                .orElse(false);
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

    public List<Integer> getAlliancesAttackingPositions(final Alliance alliance) {
        return alliance.isWhite() ? whiteAttackingPositions : blackAttackingPositions;
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
