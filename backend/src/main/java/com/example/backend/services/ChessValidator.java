package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.utils.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for validating chess-related actions, such as move validation.
 * This class provides methods to validate the legality of chess moves and board positions.
 */
@Service
public class ChessValidator {

    /**
     * Validates whether the given position is a valid position on the chessboard.
     *
     * @param position The position to be validated.
     * @throws ChessException If the position is invalid.
     */
    public void validatePosition(final int position) {
        if (!ChessUtils.isValidPosition(position)) {
            throw new ChessException(position + " is not a valid position", ChessExceptionCodes.INVALID_POSITION);
        }
    }

    /**
     * Validates the input for a chess move. This includes checking if the starting and target positions
     * are valid, if a piece exists at the starting position, and if the target position is not occupied by
     * a friendly piece.
     *
     * @param board The current board state.
     * @param move The move to be validated.
     * @throws ChessException If any of the validation checks fail.
     */
    public void makeMoveInputValidator(final Board board, final Move move) {
        // validate the positions involved in the move
        validatePosition(move.getFromTileIndex());
        validatePosition(move.getToTileIndex());

        // check if there is a piece at the starting tile
        if (board.getTileAtCoordinate(move.getFromTileIndex()).isEmpty()) {
            throw new ChessException("No piece is at the selected starting tile", ChessExceptionCodes.INVALID_MOVE);
        }

        // check if the destination tile is occupied by a friendly piece
        if (board.getAllianceOfPieceAtPosition(move.getToTileIndex()) == board.getAllianceOfPieceAtPosition(move.getFromTileIndex())) {
            throw new ChessException("Can not move to a tile occupied by a friendly piece", ChessExceptionCodes.INVALID_MOVE);
        }
    }
}
