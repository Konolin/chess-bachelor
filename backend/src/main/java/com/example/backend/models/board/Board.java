package com.example.backend.models.board;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveHistoryEntry;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Pawn;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.Rook;
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

    private final List<Tile> tiles;
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
        this.tiles = this.createTiles(builder);
        this.moveMaker = builder.moveMaker;

        this.enPassantPawnPosition = builder.enPassantPawnPosition;

        // initialize the BitBoards object
        this.piecesBitBoards = new PiecesBitBoards(builder.boardConfig);

        // calculate castle capabilities for both sides (used for fen string generation)
        calculateCastleCapabilities();

        this.whiteLegalMoves = calculateAlliancesLegalMoves(Alliance.WHITE);
        this.blackLegalMoves = calculateAlliancesLegalMoves(Alliance.BLACK);

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

    public List<Move> calculateAlliancesLegalMoves(final Alliance alliance) {
        List<Move> legalMoves = new ArrayList<>();
        long piecesBitBoard = alliance.isWhite() ? piecesBitBoards.getWhitePieces() : piecesBitBoards.getBlackPieces();
        while (piecesBitBoard != 0) {
            final int tileIndex = Long.numberOfTrailingZeros(piecesBitBoard);
            legalMoves.addAll(tiles.get(tileIndex).getOccupyingPiece().generateLegalMovesList(this));
            piecesBitBoard &= piecesBitBoard - 1;
        }
        return legalMoves;
    }

    private long calculateLegalMovesBitBoard(final Alliance alliance) {
        long attackingPositionsBitBoard = 0L;
        long piecesBitBoard = alliance.isWhite() ? piecesBitBoards.getWhitePieces() : piecesBitBoards.getBlackPieces();

        while (piecesBitBoard != 0) {
            final int tileIndex = Long.numberOfTrailingZeros(piecesBitBoard);
            attackingPositionsBitBoard |= tiles.get(tileIndex).getOccupyingPiece().generateLegalMovesBitBoard(this);
            piecesBitBoard &= piecesBitBoard - 1;
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

        // get the pieces involved in this move
        Piece movingPiece = getTileAtCoordinate(fromTileIndex).getOccupyingPiece();
        Piece capturedPiece = getTileAtCoordinate(toTileIndex).getOccupyingPiece();

        // create the move history entry
        MoveHistoryEntry moveHistoryEntry = new MoveHistoryEntry(move, movingPiece, capturedPiece, movingPiece.isFirstMove(), enPassantPawnPosition);

        // update the bitboard of the moving piece
        piecesBitBoards.updateMove(move, movingPiece);

        // update the moving piece
        movingPiece.setPosition(toTileIndex);
        movingPiece.setFirstMove(false);

        // empty the staring tile
        tiles.set(fromTileIndex, Tile.getEmptyTileForPosition(fromTileIndex));

        // add the moving piece to the destination tile
        // (if it's a promotion move, set the new piece type)
        if (move.getMoveType().isPromotion()) {
            // promote the piece
            Piece promotedPiece = ChessUtils.createPieceFromTypePositionAlliace(move.getPromotedPieceType(), moveMaker, toTileIndex);

            // set the promoted piece to the destination tile
            tiles.set(toTileIndex, Tile.createTile(promotedPiece, toTileIndex));
        } else {
            // set the moving piece to the destination tile
            tiles.set(toTileIndex, Tile.createTile(movingPiece, toTileIndex));
        }

        // handle enPassant move (remove captured enPassantPawn)
        if (move.getMoveType().isEnPassant()) {
            moveHistoryEntry.setCapturedPiece(tiles.get(enPassantPawnPosition).getOccupyingPiece());
            tiles.set(enPassantPawnPosition, Tile.getEmptyTileForPosition(enPassantPawnPosition));
        }

        // update enPassantPawnPosition
        enPassantPawnPosition = move.getMoveType().isDoublePawnAdvance() ? movingPiece.getPosition() : -1;

        // handle castle move (move rook and change its firstMove flag)
        if (move.getMoveType().isCastleMove()) {
            final int newRookPosition;
            final int oldRookPosition;
            final Rook rook;
            if (move.getMoveType().isKingSideCastle()) {
                newRookPosition = fromTileIndex + 1;
                oldRookPosition = fromTileIndex + 3;
                rook = new Rook(fromTileIndex + 1, moveMaker, false);
                tiles.set(oldRookPosition, Tile.getEmptyTileForPosition(fromTileIndex));
            } else {
                newRookPosition = fromTileIndex - 1;
                oldRookPosition = fromTileIndex - 4;
                rook = new Rook(fromTileIndex - 1, moveMaker, false);
                tiles.set(oldRookPosition, Tile.getEmptyTileForPosition(fromTileIndex));
            }
            tiles.set(newRookPosition, Tile.createTile(rook, newRookPosition));
        }

        // update castle capabilities
        calculateCastleCapabilities();

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

        // get the last executed move
        final Move move = moveHistoryEntry.getMove();
        final int fromTileIndex = move.getFromTileIndex();
        final int toTileIndex = move.getToTileIndex();

        // get the pieces involved in this move
        Piece movingPiece = moveHistoryEntry.getMovingPiece();
        Piece capturedPiece = moveHistoryEntry.getCapturedPiece();
        boolean movingPieceFirstMove = moveHistoryEntry.isFirstMove();

        // put the moving piece back to its original position and replace it in the pieces list
        movingPiece.setPosition(fromTileIndex);
        movingPiece.setFirstMove(movingPieceFirstMove);
        tiles.set(fromTileIndex, Tile.createTile(movingPiece, fromTileIndex));
        tiles.set(toTileIndex, Tile.getEmptyTileForPosition(toTileIndex));

        // restore the previous enPassantPawnPosition
        enPassantPawnPosition = moveHistoryEntry.getEnPassantPawnPosition();

        // handle enPassant move (put back the captured enPassantPawn)
        if (move.getMoveType().isEnPassant()) {
            tiles.set(enPassantPawnPosition, Tile.createTile(new Pawn(enPassantPawnPosition, moveMaker.getOpponent(), false), enPassantPawnPosition));
        } else {
            // put the captured piece back to its original position
            if (capturedPiece != null) {
                tiles.set(toTileIndex, Tile.createTile(capturedPiece, toTileIndex));
            } else {
                tiles.set(toTileIndex, Tile.getEmptyTileForPosition(toTileIndex));
            }
        }

        // handle castle move (move rook and change its firstMove flag)
        if (move.getMoveType().isCastleMove()) {
            final int rookNewPosition;
            final Rook rook;
            if (move.getMoveType().isKingSideCastle()) {
                rookNewPosition = fromTileIndex + 3;
                rook = new Rook(fromTileIndex + 3, moveMaker, true);
                tiles.set(fromTileIndex + 1, Tile.getEmptyTileForPosition(fromTileIndex));
            } else {
                rookNewPosition = fromTileIndex - 4;
                rook = new Rook(fromTileIndex - 4, moveMaker, true);
                tiles.set(fromTileIndex - 1, Tile.getEmptyTileForPosition(fromTileIndex));
            }
            tiles.set(rookNewPosition, Tile.createTile(rook, rookNewPosition));
        }

        // update pieceBitBoards
        piecesBitBoards.undoMove(moveHistoryEntry);

        // update castle capabilities
        calculateCastleCapabilities();

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
                ? Long.numberOfTrailingZeros(piecesBitBoards.getWhiteKing())
                : Long.numberOfTrailingZeros(piecesBitBoards.getBlackKing());

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
        private int enPassantPawnPosition;

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

        public Board build() {
            return new Board(this);
        }
    }
}
