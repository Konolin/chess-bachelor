package com.example.backend.models.board;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.moves.Move;
import com.example.backend.models.pieces.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Board {
    private final List<Tile> tiles;
    private final List<Move> whitePlayerLegalMoves;
    private final List<Move> blackPlayerLegalMoves;
    private final List<Piece> whitePieces;
    private final List<Piece> blackPieces;
    private final Alliance moveMaker;

    private Board(Builder builder) {
        this.tiles = this.createTiles(builder);

        this.whitePieces = calculatePieces(Alliance.WHITE);
        this.blackPieces = calculatePieces(Alliance.BLACK);

        this.whitePlayerLegalMoves = this.calculateLegalMoves(Alliance.WHITE);
        this.blackPlayerLegalMoves = this.calculateLegalMoves(Alliance.BLACK);

        this.moveMaker = builder.moveMaker;
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
        List<Piece> pieces = alliance.isBlack() ? blackPieces : whitePieces;
        assert pieces != null;
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

    public List<Piece> getMoveMakersPieces() {
        return moveMaker.isWhite() ? whitePieces : blackPieces;
    }

    public List<Piece> getOpponentsPieces() {
        return moveMaker.isWhite() ? blackPieces : whitePieces;
    }

    public Tile getTileAtCoordinate(final int tileCoordinate) {
        return this.tiles.get(tileCoordinate);
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

        public Builder setStandardStartingPosition() {
            // add pawns
            for (int i = 8; i < 16; i++) {
                this.setPieceAtPosition(new Pawn(i, Alliance.BLACK, true))
                        .setPieceAtPosition(new Pawn(i + 40, Alliance.WHITE, true));
            }
            // add rooks
            this.setPieceAtPosition(new Rook(0, Alliance.BLACK, true))
                    .setPieceAtPosition(new Rook(7, Alliance.BLACK, true))
                    .setPieceAtPosition(new Rook(56, Alliance.WHITE, true))
                    .setPieceAtPosition(new Rook(63, Alliance.WHITE, true));
            // add knights
            this.setPieceAtPosition(new Knight(1, Alliance.BLACK))
                    .setPieceAtPosition(new Knight(6, Alliance.BLACK))
                    .setPieceAtPosition(new Knight(57, Alliance.WHITE))
                    .setPieceAtPosition(new Knight(62, Alliance.WHITE));
            // add bishops
            this.setPieceAtPosition(new Bishop(2, Alliance.BLACK))
                    .setPieceAtPosition(new Bishop(5, Alliance.BLACK))
                    .setPieceAtPosition(new Bishop(58, Alliance.WHITE))
                    .setPieceAtPosition(new Bishop(61, Alliance.WHITE));
            // add queens
            this.setPieceAtPosition(new Queen(3, Alliance.BLACK))
                    .setPieceAtPosition(new Queen(59, Alliance.WHITE));
            // add kings
            this.setPieceAtPosition(new King(4, Alliance.BLACK, true))
                    .setPieceAtPosition(new King(60, Alliance.WHITE, true));
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}
