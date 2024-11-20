package com.example.backend.services;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.dtos.LegalMovesDTO;
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

        board = new Board.Builder()
                .setStandardStartingPosition()
                .setMoveMaker(Alliance.WHITE)
                .build();

        // for testing purposes
//        board = FenService.createGameFromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");

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

        // validate the input position
        validator.validatePosition(position);

        final LegalMovesDTO legalMovesDTO = new LegalMovesDTO();
        final Tile candidateTile = board.getTileAtCoordinate(position);

        if (candidateTile.isOccupied()) {
            final Piece piece = candidateTile.getOccupyingPiece();
            // get the legal moves that do not result in check
            final List<Move> legalMoves = ChessUtils.filterMovesResultingInCheck(piece.generateLegalMoves(board), board);
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
