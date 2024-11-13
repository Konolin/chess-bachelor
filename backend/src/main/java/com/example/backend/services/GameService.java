package com.example.backend.services;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.dtos.LegalMovesDTO;
import com.example.backend.models.dtos.PromotionDTO;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Pawn;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.Rook;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {
    private final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final ChessValidator validator;
    @Setter
    private Board board;

    @Autowired
    public GameService(final ChessValidator validator) {
        this.validator = validator;
    }

    public BoardStateDTO initializeBoardState() {
        long startNanos = System.nanoTime();

        board = new Board.Builder()
                .setStandardStartingPosition()
                .setMoveMaker(Alliance.WHITE)
                .build();

        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(0);

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Board initialization completed in {} ms", elapsedMillis);

        return boardStateDTO;
    }

    public LegalMovesDTO getAllMovesForPosition(final int position) {
        long startNanos = System.nanoTime();

        validator.validatePosition(position);

        final LegalMovesDTO legalMovesDTO = new LegalMovesDTO();
        final Tile candidateTile = board.getTileAtCoordinate(position);

        if (candidateTile.isOccupied()) {
            final Piece piece = candidateTile.getOccupyingPiece();
            // get the legal moves that do not result in check
            final List<Move> legalMoves = filterCheckMoves(piece.generateLegalMoves(board));
            // add the castle moves if the piece is king
            if (piece.isKing()) {
                legalMoves.addAll(board.calculateAlliancesCastleMoves(board.getMoveMaker()));
            }
            legalMovesDTO.setLegalMoves(legalMoves);
        } else {
            legalMovesDTO.setLegalMoves(null);
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Legal moves for position {} calculated in {} ms", position, elapsedMillis);

        return legalMovesDTO;
    }

    public BoardStateDTO promoteToPiece(final PromotionDTO promotionDTO) {
        long startNanos = System.nanoTime();

        final Piece movingPiece = board.getTileAtCoordinate(promotionDTO.getPosition()).getOccupyingPiece();
        final Piece promotedPiece = ChessUtils.createPieceFromCharAndPosition(promotionDTO.getPieceChar(), promotionDTO.getPosition());

        Board.Builder builder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(promotedPiece)
                .setMoveMaker(board.getMoveMaker());

        board = builder.build();
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Promotion to {} completed in {} ms", promotedPiece, elapsedMillis);

        return boardStateDTO;
    }

    public BoardStateDTO makeMove(final Move move) {
        long startNanos = System.nanoTime();

        validator.makeMoveInputValidator(board, move);

        board = executeMove(move);
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Move from {} to {} of type {} completed in {} ms", move.getFromTileIndex(), move.getToTileIndex(), move.getMoveType(), elapsedMillis);

        return boardStateDTO;
    }

    public Board executeMove(final Move move) {
//        logger.info("Executing move from {} to {} of type {}", move.getFromTileIndex(), move.getToTileIndex(), move.getMoveType());
//        logger.info("Current Board: \n{}", board);

        // obtain the piece to be moved and its new state after the move
        final Piece movingPiece = board.getTileAtCoordinate(move.getFromTileIndex()).getOccupyingPiece();
//        logger.info("Moving piece: {}", movingPiece);
        final Piece movedPiece = movingPiece.movePiece(movingPiece.getAlliance(), move.getToTileIndex());

        // initialize builder and place all pieces except the one being moved
        Board.Builder boardBuilder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(movedPiece);

        // handle special moves: en passant, double pawn advance, and castling
        handleEnPassant(move, boardBuilder, movedPiece);
        handleCastleMove(move, boardBuilder);

//        logger.info("------------------------------------------------");

        // set the next move maker (switch turns) and return the new board state
        return boardBuilder
                .setMoveMaker(board.getMoveMaker().getOpponent())
                .build();
    }

    // helper method to handle en passant logic
    private void handleEnPassant(final Move move, Board.Builder boardBuilder, final Piece movedPiece) {
        if (move.getMoveType() == MoveType.EN_PASSANT) {
            boardBuilder.setEmptyTile(board.getEnPassantPawn().getPosition());
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
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() + 1, board.getMoveMaker(), false))
                        .setEmptyTile(move.getFromTileIndex() + 3);
            } else { // Queen-side castle
                boardBuilder.setPieceAtPosition(new Rook(move.getFromTileIndex() - 1, board.getMoveMaker(), false))
                        .setEmptyTile(move.getFromTileIndex() - 4);
            }
        }
    }

    private Board.Builder placePieces(final Board.Builder builder, final Piece movedPiece) {
        for (final Piece piece : board.getAlliancesPieces(board.getMoveMaker())) {
            if (!movedPiece.equals(piece)) {
                builder.setPieceAtPosition(piece);
            }
        }
        for (final Piece piece : board.getAlliancesPieces(board.getMoveMaker().getOpponent())) {
            builder.setPieceAtPosition(piece);
        }
        return builder;
    }

    private List<Move> filterCheckMoves(final List<Move> allMoves) {
        final List<Move> legalMoves = new ArrayList<>();

        for (final Move move : allMoves) {
            Board transitionBoard = executeMove(move);

            // remove moves that cause the current player to be in check.
            // the opponents alliance is checked because the move maker changes after executeMove(), which means
            // the current move maker (who's moves are filtered) is considered the opponent in the transitionBoard
            if (!transitionBoard.isAllianceInCheck(transitionBoard.getMoveMaker().getOpponent())) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    private int getWinnerFlag() {
        final Alliance moveMaker = board.getMoveMaker();
        if (board.isAllianceInCheck(moveMaker) && filterCheckMoves(board.getAlliancesLegalMoves(moveMaker)).isEmpty()) {
            return moveMaker.isWhite() ? -1 : 1;
        }
        return 0;
    }
}
