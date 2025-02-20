package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.Move;
import com.example.backend.models.pieces.*;
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
    private final List<Piece> whitePieces;
    private final List<Piece> blackPieces;
    private List<Tile> tiles;
    private Alliance moveMaker;
    private List<Move> whiteLegalMoves;
    private List<Move> blackLegalMoves;
    private long whiteLegalMovesBitBoard;
    private long blackLegalMovesBitBoard;
    private Pawn enPassantPawn;
    private PiecesBitBoards piecesBitBoards;
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
        return new ArrayList<>(Arrays.asList(tilesArray));
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
            if (piece.getType() == PieceType.PAWN) {
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

    public void executeMove(final Move move) {
        final int fromTileIndex = move.getFromTileIndex();
        final int toTileIndex = move.getToTileIndex();
        Piece movingPiece = getTileAtCoordinate(fromTileIndex).getOccupyingPiece();
        movingPiece.setPosition(toTileIndex);
        movingPiece.setFirstMove(false);

        // empty the staring tile
        setTileAtCoordinate(fromTileIndex, Tile.getEmptyTileForPosition(fromTileIndex));

        // set the moved piece to the destination tile
        if (move.getMoveType().isPromotion()) {
            // promote the piece
            movingPiece = ChessUtils.createPieceFromTypePositionAlliace(move.getPromotedPieceType(), moveMaker, toTileIndex);
        }
        setTileAtCoordinate(toTileIndex, Tile.createTile(movingPiece, toTileIndex));

        // handle enPassant move (remove captured enPassantPawn)
        if (move.getMoveType().isEnPassant()) {
            // remove the enPassantPawn
            for (final Piece piece : getAlliancesPieces(moveMaker.getOpponent())) {
                if (piece.getPosition() == enPassantPawn.getPosition()) {
                    if (moveMaker.isWhite()) {
                        whitePieces.remove(piece);
                    } else {
                        blackPieces.remove(piece);
                    }
                    break;
                }
            }
        }

        // update enPassantPawn
        enPassantPawn = move.getMoveType().isDoublePawnAdvance() ? (Pawn) movingPiece : null;

        // update moveMakers pieces (remove the old piece, add the new one)
        for (final Piece piece : getAlliancesPieces(moveMaker)) {
            // replace the piece the new one (a new piece was created to make it simpler in case of promotions)
            if (piece.getPosition() == fromTileIndex) {
                if (moveMaker.isWhite()) {
                    whitePieces.set(whitePieces.indexOf(piece), movingPiece);
                } else {
                    blackPieces.set(blackPieces.indexOf(piece), movingPiece);
                }
                break;
            }
        }

        // update opponents pieces (remove the captured piece)
        for (final Piece piece : getAlliancesPieces(moveMaker.getOpponent())) {
            if (piece.getPosition() == toTileIndex) {
                if (moveMaker.isWhite()) {
                    whitePieces.remove(piece);
                } else {
                    blackPieces.remove(piece);
                }
                break;
            }
        }

        // handle castle move (move rook and change its firstMove flag)
        if (move.getMoveType().isCastleMove()) {
            final int rookNewPosition;
            final Rook rook;
            if (move.getMoveType().isKingSideCastle()) {
                rookNewPosition = fromTileIndex + 1;
                rook = new Rook(fromTileIndex + 1, moveMaker, false);
                tiles.set(fromTileIndex + 3, Tile.getEmptyTileForPosition(fromTileIndex));
            } else {
                rookNewPosition = fromTileIndex - 1;
                rook = new Rook(fromTileIndex - 1, moveMaker, false);
                tiles.set(fromTileIndex - 4, Tile.getEmptyTileForPosition(fromTileIndex));
            }
            tiles.set(rookNewPosition, Tile.createTile(rook, rookNewPosition));
        }

        // update pieceBitBoards
        piecesBitBoards = new PiecesBitBoards(tiles);

        // update legal moves
        whiteLegalMoves = calculateLegalMoves(Alliance.WHITE);
        blackLegalMoves = calculateLegalMoves(Alliance.BLACK);

        // update legalMovesBitBoards
        whiteLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.WHITE);
        blackLegalMovesBitBoard = calculateLegalMovesBitBoard(Alliance.BLACK);

        // update castle capabilities if the moved piece was a castle partaker piece (king or rook)
        if (movingPiece.getType().isCastlePartaker()) {
            calculateCastleCapabilities();
        }

        // change moveMaker
        moveMaker = moveMaker.getOpponent();
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

    public void setTileAtCoordinate(final int tileCoordinate, final Tile tile) {
        tiles.set(tileCoordinate, tile);
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
