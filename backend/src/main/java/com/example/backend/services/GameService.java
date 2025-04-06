package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.moves.MoveDTO;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Piece;
import com.example.backend.models.pieces.PieceType;
import com.example.backend.utils.CastleUtils;
import com.example.backend.utils.ChessUtils;
import com.example.backend.utils.MoveUtils;
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
    private Board board;

    static {
        MoveSearch.init(512);
    }

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
        this.board = FenService.createGameFromFEN(ChessUtils.STARTING_FEN);

        final BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(this.board));
        boardStateDTO.setWinnerFlag(0);

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
    public List<MoveDTO> getAllMovesForPosition(final int position) {
        // validate the input position
        this.validator.validatePosition(position);

        MoveList legalMoves;

        if (this.board.isTileOccupied(position)) {
            final PieceType pieceTypeAtPosition = this.board.getPieceTypeAtPosition(position);

            // get the moves and filter the ones that result in the check of the current player
            legalMoves = Piece.generateLegalMovesList(this.board, position, this.board.getMoveMaker(),
                    pieceTypeAtPosition, this.board.getAlliancesLegalMovesBBs(this.board.getMoveMaker()).get(position));
            legalMoves = ChessUtils.filterMovesResultingInCheck(legalMoves, this.board.getPiecesBBs(),
                    this.board.getEnPassantPawnPosition(), this.board.getMoveMaker().getOpponent());

            // add the castle moves if the piece is a king
            if (pieceTypeAtPosition == PieceType.KING) {
                legalMoves.addAll(CastleUtils.calculateCastleMoves(this.board, this.board.getMoveMaker()));
            }
        } else {
            legalMoves = null;
        }

        return MoveUtils.createMoveDTOListFromMoveList(legalMoves);
    }

    /**
     * Executes a move on the board after validating the move input.
     * The board is updated with the new move, and the BoardStateDTO is returned with the updated FEN and winner flag.
     *
     * @param moveDTO The moveDTO to be executed on the board.
     * @return A BoardStateDTO with the updated board state and winner flag.
     */
    public BoardStateDTO makeMove(final MoveDTO moveDTO) {
        final int move = MoveUtils.createMoveFromMoveDTO(moveDTO);
        this.validator.makeMoveInputValidator(this.board, move);

        this.board.executeMove(move);
        final BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(this.board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());

        return boardStateDTO;
    }

    /**
     * Undoes the last move made in the game.
     * The board is updated by undoing the last move, and the BoardStateDTO is returned with the updated FEN and winner flag.
     *
     * @return A BoardStateDTO with the updated board state and winner flag.
     */
    public BoardStateDTO undoLastMove() {
        this.board.undoLastMove();
        final BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(this.board));
        boardStateDTO.setWinnerFlag(0);

        return boardStateDTO;
    }

    /**
     * Determines if there is a winner in the current game state.
     * A player wins if their king is in check and they have no legal moves left.
     *
     * @return An integer representing the winner flag:
     * -1 for White's win,
     * 1 for Black's win,
     * 0 if there is no winner yet.
     */
    private int getWinnerFlag() {
        final Alliance moveMaker = this.board.getMoveMaker();
        if (this.board.isAllianceInCheckMate(moveMaker)) {
            return moveMaker.isWhite() ? -1 : 1;
        }
        return 0;  // No winner yet
    }

    /**
     * Uses the MoveFinder service to get the best move by using the AI model and alpha beta pruning algorithm
     * After the best move is found, the move is executed
     *
     * @return BoardStateDTO object containing the new state
     */
    public BoardStateDTO computerMakeMove() {
        final int move = MoveSearch.findBestMove(this.board, 3);
        this.board.executeMove(move);
        final BoardStateDTO boardStateDTO = new BoardStateDTO();
        boardStateDTO.setFen(FenService.createFENFromGame(this.board));
        boardStateDTO.setWinnerFlag(getWinnerFlag());
        return boardStateDTO;
    }
}
