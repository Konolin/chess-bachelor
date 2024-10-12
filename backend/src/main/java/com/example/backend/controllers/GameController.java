package com.example.backend.controllers;

import com.example.backend.models.BoardState;
import com.example.backend.models.ChessConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {
    @GetMapping("/starting-board-state")
    public ResponseEntity<BoardState> getStartingBoardState() {
        int s = 0;
        return ResponseEntity.ok(new BoardState(ChessConstants.STARTING_BOARD_FEN));
    }
}
