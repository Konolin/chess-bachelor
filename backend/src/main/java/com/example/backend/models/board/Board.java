package com.example.backend.models.board;

import com.example.backend.models.ChessConstants;
import com.example.backend.models.Color;
import com.example.backend.models.pieces.*;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Board {
    private final List<Tile> gameBoard;
    private final Collection<Piece> whitePieces;
    private final Collection<Piece> blackPieces;

    private Board(Builder builder) {
        this.gameBoard = generateGameBoard(builder);
        this.whitePieces = calculateActivePieces(this.gameBoard, Color.WHITE);
        this.blackPieces = calculateActivePieces(this.gameBoard, Color.BLACK);
    }

    private static Collection<Piece> calculateActivePieces(final List<Tile> gameBoard, final Color color) {
        return gameBoard.stream()
                .filter(Tile::isTileOccupied)
                .map(Tile::getPieceOnTile)
                .filter(piece -> piece != null && piece.getColor() == color)
                .collect(Collectors.toList());
    }

    public static Board createStandardBoard() {
        final Builder builder = new Builder();

        builder.setPieceAtPosition(new Rook(Color.BLACK, 0))
                .setPieceAtPosition(new Knight(Color.BLACK, 1))
                .setPieceAtPosition(new Bishop(Color.BLACK, 2))
                .setPieceAtPosition(new Queen(Color.BLACK, 3))
                .setPieceAtPosition(new King(Color.BLACK, 4))
                .setPieceAtPosition(new Bishop(Color.BLACK, 5))
                .setPieceAtPosition(new Knight(Color.BLACK, 6))
                .setPieceAtPosition(new Rook(Color.BLACK, 7))
                .setPieceAtPosition(new Rook(Color.WHITE, 56))
                .setPieceAtPosition(new Knight(Color.WHITE, 57))
                .setPieceAtPosition(new Bishop(Color.WHITE, 58))
                .setPieceAtPosition(new Queen(Color.WHITE, 59))
                .setPieceAtPosition(new King(Color.WHITE, 60))
                .setPieceAtPosition(new Bishop(Color.WHITE, 61))
                .setPieceAtPosition(new Knight(Color.WHITE, 62))
                .setPieceAtPosition(new Rook(Color.WHITE, 63));

        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(Color.BLACK, i + 8));
        }
        for (int i = 0; i < 8; i++) {
            builder.setPieceAtPosition(new Pawn(Color.WHITE, i + 48));
        }

        builder.setNextMoveMaker(Color.WHITE);
        return builder.build();
    }

    private List<Tile> generateGameBoard(final Builder builder) {
        final Tile[] tiles = new Tile[ChessConstants.TOTAL_TILES];
        for (int i = 0; i < ChessConstants.TOTAL_TILES; i++) {
            tiles[i] = Tile.createTile(i, builder.boardConfig.get(i));
        }
        return List.of(tiles);
    }

    public static class Builder {
        private final Map<Integer, Piece> boardConfig;
        private Color nextMoveMaker;

        public Builder() {
            this.boardConfig = new HashMap<>();
        }

        public Builder setPieceAtPosition(final Piece piece) {
            this.boardConfig.put(piece.getPosition(), piece);
            return this;
        }

        public Builder setNextMoveMaker(final Color nextMoveMaker) {
            this.nextMoveMaker = nextMoveMaker;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}