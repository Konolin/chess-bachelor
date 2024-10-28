package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.board.Tile;
import com.example.backend.models.dtos.LegalMovesDTO;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        return boardStateDTO;
    }

    public LegalMovesDTO getAllMovesForPosition(final int position) {
        LegalMovesDTO legalMovesDTO = new LegalMovesDTO();

        final Tile candidateTile = board.getTileAtCoordinate(position);
        if (candidateTile.isOccupied()) {
            legalMovesDTO.setLegalMoves(candidateTile.getOccupyingPiece().generateLegalMoves(board));
        } else {
            legalMovesDTO.setLegalMoves(null);
        }

        return legalMovesDTO;
    }

    public BoardStateDTO makeMove(final int fromTilePosition, final int toTilePosition) {
        validator.makeMoveValidator(board, fromTilePosition, toTilePosition);

        final Piece movedPiece = board.getTileAtCoordinate(fromTilePosition).getOccupyingPiece();
        board = placePieces(new Board.Builder(), movedPiece)
                .setPieceAtPosition(movedPiece.movePiece(movedPiece.getAlliance(), toTilePosition))
                .setMoveMaker(board.getMoveMaker().getOpponent())
                .build();

        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));

        return boardStateDTO;
    }

    private Board.Builder placePieces(final Board.Builder builder, final Piece movedPiece) {
        for (final Piece piece: board.getMoveMakersPieces()) {
            if (!movedPiece.equals(piece)) {
                builder.setPieceAtPosition(piece);
            }
        }
        for (final Piece piece : board.getOpponentsPieces()) {
            builder.setPieceAtPosition(piece);
        }
        return builder;
    }
}
