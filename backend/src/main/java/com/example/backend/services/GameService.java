package com.example.backend.services;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.moves.Move;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");

        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(0);

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Board initialization completed in {} ms", elapsedMillis);

        return boardStateDTO;
    }

    public List<Move> getAllMovesForPosition(final int position) {
        long startNanos = System.nanoTime();

        // validate the input position
        validator.validatePosition(position);

        final Tile candidateTile = board.getTileAtCoordinate(position);
        final List<Move> legalMoves;

        if (candidateTile.isOccupied()) {
            final Piece piece = candidateTile.getOccupyingPiece();
            // get the legal moves that do not result in check
            legalMoves = ChessUtils.filterMovesResultingInCheck(piece.generateLegalMoves(board), board);
            // add the castle moves if the piece is king
            if (piece.isKing()) {
                legalMoves.addAll(board.calculateAlliancesCastleMoves(board.getMoveMaker()));
            }
        } else {
            legalMoves = null;
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Legal moves for position {} calculated in {} ms", position, elapsedMillis);

        return legalMoves;
    }

    public BoardStateDTO makeMove(final Move move) {
        long startNanos = System.nanoTime();

        validator.makeMoveInputValidator(board, move);

        board = board.executeMove(move);
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        logger.info("Move from {} to {} of type {} completed in {} ms", move.getFromTileIndex(), move.getToTileIndex(), move.getMoveType(), elapsedMillis);

        return boardStateDTO;
    }

    private int getWinnerFlag() {
        final Alliance moveMaker = board.getMoveMaker();
        if (board.isAllianceInCheck(moveMaker) && board.getAlliancesLegalMoves(moveMaker).isEmpty()) {
            return moveMaker.isWhite() ? -1 : 1;
        }
        return 0;
    }
}
