package com.example.backend.services;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.dtos.LegalMovesDTO;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.dtos.PromotionDTO;
import com.example.backend.models.moves.Move;
import com.example.backend.models.moves.MoveType;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Pawn;
import com.example.backend.models.pieces.Piece;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {
    private Board board;
    private final ChessValidator validator;

    @Autowired
    public GameService(final ChessValidator validator) {
        this.validator = validator;
    }

    public BoardStateDTO initializeBoardState() {
        board = new Board.Builder()
                .setStandardStartingPosition()
                .setMoveMaker(Alliance.WHITE)
                .build();

        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(0);

        return boardStateDTO;
    }

    public LegalMovesDTO getAllMovesForPosition(final int position) {
        validator.validatePosition(position);

        LegalMovesDTO legalMovesDTO = new LegalMovesDTO();
        final Tile candidateTile = board.getTileAtCoordinate(position);

        if (candidateTile.isOccupied()) {
            legalMovesDTO.setLegalMoves(filterCheckMoves(candidateTile.getOccupyingPiece().generateLegalMoves(board)));
        } else {
            legalMovesDTO.setLegalMoves(null);
        }

        return legalMovesDTO;
    }

    public BoardStateDTO promoteToPiece(final PromotionDTO promotionDTO) {
        final Piece movingPiece = board.getTileAtCoordinate(promotionDTO.getPosition()).getOccupyingPiece();
        final Piece promotedPiece = ChessUtils.createPieceFromCharAndPosition(promotionDTO.getPieceChar(), promotionDTO.getPosition());

        Board.Builder builder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(promotedPiece)
                .setMoveMaker(board.getMoveMaker());

        board = builder.build();
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        return boardStateDTO;
    }

    public BoardStateDTO makeMove(final Move move) {
        validator.makeMoveInputValidator(board, move);

        board = executeMove(move);
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        return boardStateDTO;
    }

    private Board executeMove(final Move move) {
        // the piece that is going to be moved
        final Piece movingPiece = board.getTileAtCoordinate(move.getFromTileIndex()).getOccupyingPiece();
        // the piece after it was moved to the new position
        final Piece movedPiece = movingPiece.movePiece(movingPiece.getAlliance(), move.getToTileIndex());

        Board.Builder builder = placePieces(new Board.Builder(), movingPiece)
                .setPieceAtPosition(movedPiece)
                .setMoveMaker(board.getMoveMaker().getOpponent());

        if (move.getMoveType() == MoveType.EN_PASSANT) {
            builder.setEmptyTile(board.getEnPassantPawn().getPosition());
        }

        // check if there needs to be an en passant pawn saved
        builder.setEnPassantPawn(move.getMoveType() == MoveType.DOUBLE_PAWN_ADVANCE ? (Pawn) movedPiece : null);

        return builder.build();
    }

    private Board.Builder placePieces(final Board.Builder builder, final Piece movedPiece) {
        for (final Piece piece: board.getAlliancesPieces(board.getMoveMaker())) {
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
        if (board.isAllianceInCheck(moveMaker) && filterCheckMoves(board.getAllianceLegalMoves(moveMaker)).isEmpty()) {
            return moveMaker.isWhite() ? -1 : 1;
        }
        return 0;
    }
}
