package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.ChessUtils;
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

    private boolean isBlackKingSideCastleCapable;
    private boolean isBlackQueenSideCastleCapable;
    private boolean isWhiteKingSideCastleCapable;
    private boolean isWhiteQueenSideCastleCapable;

    private Board(Builder builder) {
        this.tiles = this.createTiles(builder);

        this.whitePieces = calculatePieces(Alliance.WHITE);
        this.blackPieces = calculatePieces(Alliance.BLACK);
        this.enPassantPawn = builder.enPassantPawn;

        this.whiteLegalMoves = calculateLegalMoves(Alliance.WHITE);
        this.blackLegalMoves = calculateLegalMoves(Alliance.BLACK);

        this.whiteAttackingPositions = calculateAttackingPositions(Alliance.WHITE);
        this.blackAttackingPositions = calculateAttackingPositions(Alliance.BLACK);

        this.moveMaker = builder.moveMaker;

        calculateCastleCapabilities();
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
            legalMoves.addAll(piece.generateLegalMoves(this));
        }
        return legalMoves;
    }

    private List<Piece> calculatePieces(final Alliance alliance) {
        return tiles.stream()
                .filter(Tile::isOccupied)
                .map(Tile::getOccupyingPiece)
                .filter(piece -> piece != null && piece.getAlliance() == alliance)
                .toList();
    }

    private List<Integer> calculateAttackingPositions(final Alliance alliance) {
        List<Integer> attackingPositions = new ArrayList<>();

        // calculate pawn attacks
        for (final Piece piece : (alliance.isWhite() ? whitePieces : blackPieces)) {
            if (piece.isPawn()) {
                int pos = piece.getPosition();
                int attack1 = alliance.isWhite() ? pos - 7 : pos + 7;
                int attack2 = alliance.isWhite() ? pos - 9 : pos + 9;
                int column = pos % 8;

                // check attack1 (right diagonal for white, left diagonal for black)
                if (ChessUtils.isValidPosition(attack1) && column != 7) {
                    attackingPositions.add(attack1);
                }

                // check attack2 (left diagonal for white, right diagonal for black)
                if (ChessUtils.isValidPosition(attack2) && column != 0) {
                    attackingPositions.add(attack2);
                }
            }
        }

        // calculate legal move attacks for all pieces
        for (final Move move : (alliance.isWhite() ? whiteLegalMoves : blackLegalMoves)) {
            attackingPositions.add(move.getToTileIndex());
        }

        return attackingPositions.stream().distinct().toList();
    }


    private void calculateCastleCapabilities() {
        isBlackKingSideCastleCapable = false;
        isBlackQueenSideCastleCapable = false;
        isWhiteKingSideCastleCapable = false;
        isWhiteQueenSideCastleCapable = false;

        if (getEligibleKingForCastle(Alliance.WHITE) != null) {
            for (final Rook rook : getEligibleRooksForCastle(Alliance.WHITE)) {
                if (rook.getPosition() == 63) {
                    isWhiteKingSideCastleCapable = true;
                } else {
                    isWhiteQueenSideCastleCapable = true;
                }
            }
        }

        if (getEligibleKingForCastle(Alliance.BLACK) != null) {
            for (final Rook rook : getEligibleRooksForCastle(Alliance.BLACK)) {
                if (rook.getPosition() == 7) {
                    isBlackKingSideCastleCapable = true;
                } else {
                    isBlackQueenSideCastleCapable = true;
                }
            }
        }
    }

    private boolean isAllianceCastleCapable(final Alliance alliance) {
        if (alliance.isWhite()) {
            return isWhiteKingSideCastleCapable || isWhiteQueenSideCastleCapable;
        }
        return isBlackKingSideCastleCapable || isBlackQueenSideCastleCapable;
    }

    public List<Move> calculateAlliancesCastleMoves(final Alliance alliance) {
        final List<Move> castleMoves = new ArrayList<>();

        // return early if the move maker is not castle eligible
        if (!isAllianceCastleCapable(alliance)) {
            return castleMoves;
        }

        final List<Rook> rooks = getEligibleRooksForCastle(alliance);

        // check if the squares between the rook and king are safe
        final int kingPosition = getEligibleKingForCastle(alliance).getPosition();
        for (final Rook rook : rooks) {
            if (rook.getPosition() < kingPosition) {
                if (areTilesEligibleForCastle(kingPosition, new int[]{-1, -2, -3})) {
                    castleMoves.add(new Move(kingPosition, kingPosition - 2, MoveType.QUEEN_SIDE_CASTLE));
                }
            } else {
                if (areTilesEligibleForCastle(kingPosition, new int[]{1, 2})) {
                    castleMoves.add(new Move(kingPosition, kingPosition + 2, MoveType.KING_SIDE_CASTLE));
                }
            }
        }

        return castleMoves;
    }

    public Board executeMove(final Move move) {
        // obtain the piece that is going to be moved
        final Piece movingPiece = getTileAtCoordinate(move.getFromTileIndex()).getOccupyingPiece();

        // check if this is a promotion move
        if (move.getMoveType().isPromotion()) {
            // create the promoted piece on its new position
            final Piece promotedPiece = ChessUtils.createPieceFromCharAndPosition(move.getPromotedPieceChar(), move.getToTileIndex());

            // build the new board state with the promoted piece
            return placePieces(new Board.Builder(), movingPiece)
                    .setPieceAtPosition(promotedPiece)
                    .setMoveMaker(moveMaker.getOpponent())
                    .build();
        }

        // regular move handling
        final Piece movedPiece = movingPiece.movePiece(movingPiece.getAlliance(), move.getToTileIndex());

        // initialize builder and place all pieces except the one being moved
        Board.Builder boardBuilder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(movedPiece);

        // handle special moves: en passant, double pawn advance, and castling
        handleEnPassant(move, boardBuilder, movedPiece);
        handleCastleMove(move, boardBuilder);

        // set the next move maker (switch turns) and return the new board state
        return boardBuilder
                .setMoveMaker(moveMaker.getOpponent())
                .build();
    }

    // helper method to handle en passant logic
    private void handleEnPassant(final Move move, Board.Builder boardBuilder, final Piece movedPiece) {
        if (move.getMoveType() == MoveType.EN_PASSANT) {
            boardBuilder.setEmptyTile(enPassantPawn.getPosition());
        }

        // set the en passant pawn if this move is a double pawn advance
        if (move.getMoveType() == MoveType.DOUBLE_PAWN_ADVANCE) {
            boardBuilder.setEnPassantPawn((Pawn) movedPiece);
        } else {
            boardBuilder.setEnPassantPawn(null);
        }
    }

    // helper method to handle castling logic
    private void handleCastleMove(final Move move, Board.Builder boardBuilder) {
        if (move.getMoveType().isCastleMove()) {
            if (move.getMoveType() == MoveType.KING_SIDE_CASTLE) {
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() + 1, moveMaker, false))
                        .setEmptyTile(move.getFromTileIndex() + 3);
            } else { // Queen-side castle
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() - 1, moveMaker, false))
                        .setEmptyTile(move.getFromTileIndex() - 4);
            }
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

    // helper method to check if the tiles between rook and king are eligible for castling
    private boolean areTilesEligibleForCastle(final int kingPosition, final int[] offsets) {
        for (int offset : offsets) {
            // check if tile is occupied
            if (getTileAtCoordinate(kingPosition + offset).isOccupied()) {
                return false;
            }
            // check if tile is attacked by opponent (offset -3 does not need to be checked for attacks)
            if (offset != -3 && getAlliancesAttackingPositions(moveMaker.getOpponent()).contains(kingPosition + offset)) {
                return false;
            }
        }
        return true;
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

    public Tile getTileAtCoordinate(final int tileCoordinate) {
        return this.tiles.get(tileCoordinate);
    }

    public List<Move> getAlliancesLegalMoves(final Alliance alliance) {
        return alliance.isWhite()
                ? ChessUtils.filterMovesResultingInCheck(whiteLegalMoves, this)
                : ChessUtils.filterMovesResultingInCheck(blackLegalMoves, this);
    }

    private List<Integer> getAlliancesAttackingPositions(final Alliance alliance) {
        return alliance.isWhite() ? whiteAttackingPositions : blackAttackingPositions;
    }

    private King getEligibleKingForCastle(final Alliance alliance) {
        return getAlliancesPieces(alliance).stream()
                .filter(piece -> piece.isKing() && piece.isFirstMove() &&
                        !getAlliancesAttackingPositions(alliance.getOpponent()).contains(piece.getPosition()))
                .map(King.class::cast)
                .findFirst()
                .orElse(null);
    }

    private List<Rook> getEligibleRooksForCastle(final Alliance alliance) {
        return getAlliancesPieces(alliance).stream()
                .filter(piece -> piece.isRook() && piece.isFirstMove())
                .map(Rook.class::cast)
                .toList();
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
