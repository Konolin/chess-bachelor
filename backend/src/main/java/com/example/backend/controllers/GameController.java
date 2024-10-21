package com.example.backend.controllers;

import com.example.backend.models.dtos.AllMovesDTO;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/get-moves-for-position")
    public ResponseEntity<AllMovesDTO> getAllMovesForPosition(@RequestParam Integer tileIndex) {
        return ResponseEntity.ok(gameService.getAllMovesForPosition(tileIndex));
    }

    @GetMapping("make-move")
    public ResponseEntity<BoardStateDTO> makeMove(@RequestParam Integer fromTileIndex, @RequestParam Integer toTileIndex) {
        return ResponseEntity.ok(gameService.makeMove(fromTileIndex, toTileIndex));
    }

    @GetMapping("/starting-board-state")
    public ResponseEntity<BoardStateDTO> getStartingBoardState() {
        return ResponseEntity.ok(gameService.initializeBoardState());
    }
}
