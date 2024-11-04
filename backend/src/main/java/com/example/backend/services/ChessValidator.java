package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import org.springframework.stereotype.Service;

@Service
public class ChessValidator {
    public void validatePosition(final int position) {
        if (!ChessUtils.isValidPosition(position)) {
            throw new ChessException(position + " is not a valid position", ChessExceptionCodes.INVALID_POSITION);
        }
    }

    public void makeMoveInputValidator(final Board board, final Move move) {
        validatePosition(move.getFromTileIndex());
        validatePosition(move.getToTileIndex());

        if (board.getTileAtCoordinate(move.getFromTileIndex()).isEmpty()) {
            throw new ChessException("No piece is at the selected starting tile", ChessExceptionCodes.INVALID_MOVE);
        }

        if (board.getAllianceOfPieceAtPosition(move.getToTileIndex()) == board.getAllianceOfPieceAtPosition(move.getFromTileIndex())) {
            throw new ChessException("Can not move to a tile occupied by a friendly piece", ChessExceptionCodes.INVALID_MOVE);
        }
    }
}
