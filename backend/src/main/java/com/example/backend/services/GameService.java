package com.example.backend.services;

import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
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

/**
 * The GameService class provides services to manage and manipulate a chess game.
 * It includes functionality for initializing the board, validating moves, executing moves,
 * and determining the winner.
 */
@Service
public class GameService {

    private final ChessValidator validator;
    @Setter
    private Board board;

    /**
     * Constructor for GameService that initializes the validator.
     *
     * @param validator A ChessValidator instance to validate moves.
     */
    @Autowired
    public GameService(final ChessValidator validator) {
        this.validator = validator;
    }

    /**
     * Initializes the game by creating a new chessboard from the starting FEN string.
     * Sets up the initial state of the board and returns the BoardStateDTO.
     *
     * @return A BoardStateDTO containing the initial board setup in FEN format and winner flag.
     */
    public BoardStateDTO initializeBoardState() {
        board = FenService.createGameFromFEN(ChessUtils.STARTING_FEN);

        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(0);  // No winner initially

        return boardStateDTO;
    }

    /**
     * Retrieves a list of all legal moves for a given position on the board.
     * The legal moves are filtered to ensure no move results in a check.
     * If the piece is a King, castle moves are also included.
     *
     * @param position The board position (coordinate) for which to retrieve legal moves.
     * @return A list of legal moves for the piece at the given position.
     */
    public List<Move> getAllMovesForPosition(final int position) {
        // validate the input position
        validator.validatePosition(position);

        final Tile candidateTile = board.getTileAtCoordinate(position);
        final List<Move> legalMoves;

        if (candidateTile.isOccupied()) {
            final Piece piece = candidateTile.getOccupyingPiece();
            // get the legal moves that do not result in check
            legalMoves = ChessUtils.filterMovesResultingInCheck(piece.generateLegalMovesList(board), board);
            // add the castle moves if the piece is a king
            if (piece.isKing()) {
                legalMoves.addAll(CastleUtils.calculateCastleMoves(board, board.getMoveMaker()));
            }
        } else {
            legalMoves = null;
        }

        return legalMoves;
    }

    /**
     * Executes a move on the board after validating the move input.
     * The board is updated with the new move, and the BoardStateDTO is returned with the updated FEN and winner flag.
     *
     * @param move The move to be executed on the board.
     * @return A BoardStateDTO with the updated board state and winner flag.
     */
    public BoardStateDTO makeMove(final Move move) {
        validator.makeMoveInputValidator(board, move);

        board = board.executeMove(move);
        BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        return boardStateDTO;
    }

    /**
     * Determines if there is a winner in the current game state.
     * A player wins if their king is in check and they have no legal moves left.
     *
     * @return An integer representing the winner flag:
     *         -1 for White's win,
     *         1 for Black's win,
     *         0 if there is no winner yet.
     */
    private int getWinnerFlag() {
        final Alliance moveMaker = board.getMoveMaker();
        if (board.isAllianceInCheck(moveMaker) && board.getAlliancesLegalMoves(moveMaker).isEmpty()) {
            return moveMaker.isWhite() ? -1 : 1;
        }
        return 0;  // No winner yet
    }
}
