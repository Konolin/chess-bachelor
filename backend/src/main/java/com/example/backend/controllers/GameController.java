package com.example.backend.controllers;

import com.example.backend.models.BoardState;
import com.example.backend.models.ChessConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {
    @GetMapping("/starting-board-state")
    public ResponseEntity<BoardState> getStartingBoardState() {
        // TODO - temporary
        return ResponseEntity.ok(new BoardState(ChessConstants.STARTING_BOARD_FEN));
    }

    @GetMapping("/get-legal-moves-indexes")
    public ResponseEntity<Integer[]> getLegalMovesIndexes(@RequestParam Integer tileIndex) {
        // TODO - temporary
        return ResponseEntity.ok(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7});
    }
}
