package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import org.springframework.stereotype.Service;

@Service
public class ChessValidator {
    public void makeMoveValidator(final Board board, final int fromTilePosition, final int toTilePosition) {
        if (!ChessUtils.isValidPosition(fromTilePosition)) {
            throw new ChessException("fromTilePosition is not a valid position", ChessExceptionCodes.INVALID_POSITION);
        }

        if (!ChessUtils.isValidPosition(toTilePosition)) {
            throw new ChessException("toTilePosition is not a valid position", ChessExceptionCodes.INVALID_POSITION);
        }

        if (board.getTileAtCoordinate(fromTilePosition).isEmpty()) {
            throw new ChessException("No piece is at the selected starting tile", ChessExceptionCodes.INVALID_MOVE);
        }
    }
}
