package com.example.backend.controllers;

import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.moves.MoveDTO;
import com.example.backend.services.GameService;
import com.example.backend.services.MoveFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The {@code GameController} class is a Spring Boot REST controller that handles API endpoints for managing
 * a chess game. It provides endpoints to fetch possible moves, make a move, and initialize the game board.
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {

    /**
     * Service layer dependency for game-related operations.
     */
    private final GameService gameService;

    /**
     * Constructs a {@code GameController} with the required {@code GameService} dependency.
     *
     * @param gameService the service layer object for chess game operations
     */
    @Autowired
    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Retrieves all possible moves for a piece located at a given tile index.
     *
     * @param tileIndex the index of the tile for which to get possible moves
     * @return a {@code ResponseEntity} containing a list of {@code MoveDTO} objects
     */
    @GetMapping("/get-moves-for-position")
    public ResponseEntity<List<MoveDTO>> getAllMovesForPosition(@RequestParam Integer tileIndex) {
        return ResponseEntity.ok(gameService.getAllMovesForPosition(tileIndex));
    }

    /**
     * Executes a move and updates the game state.
     *
     * @param moveDTO the {@code MoveDTO} object representing the move to be made
     * @return a {@code ResponseEntity} containing the updated {@code BoardStateDTO}
     */
    @PostMapping("make-move")
    public ResponseEntity<BoardStateDTO> makeMove(@RequestBody MoveDTO moveDTO) {
        return ResponseEntity.ok(gameService.makeMove(moveDTO));
    }

    /**
     * Executes a move for the computer player and updates the game state.
     *
     * @return a {@code ResponseEntity} containing the updated {@code BoardStateDTO}
     */
    @GetMapping("computer-make-move")
    public ResponseEntity<BoardStateDTO> computerMakeMove() {
        return ResponseEntity.ok(gameService.computerMakeMove());
    }

    /**
     * Retrieves the initial state of the chess board.
     *
     * @return a {@code ResponseEntity} containing the starting {@code BoardStateDTO}
     */
    @GetMapping("/starting-board-state")
    public ResponseEntity<BoardStateDTO> getStartingBoardState() {
        return ResponseEntity.ok(gameService.initializeBoardState());
    }

    /**
     * Undoes the last move made in the game.
     *
     * @return a {@code ResponseEntity} containing the updated {@code BoardStateDTO}
     */
    @GetMapping("/undo-last-move")
    public ResponseEntity<BoardStateDTO> undoLastMove() {
        return ResponseEntity.ok(gameService.undoLastMove());
    }
}
