package com.example.backend.controllers;

import com.example.backend.models.dtos.LegalMovesDTO;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.dtos.PromotionDTO;
import com.example.backend.models.moves.Move;
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
    public ResponseEntity<LegalMovesDTO> getAllMovesForPosition(@RequestParam Integer tileIndex) {
        return ResponseEntity.ok(gameService.getAllMovesForPosition(tileIndex));
    }

    @PostMapping("make-move")
    public ResponseEntity<BoardStateDTO> makeMove(@RequestBody Move move) {
        return ResponseEntity.ok(gameService.makeMove(move));
    }

    @GetMapping("/starting-board-state")
    public ResponseEntity<BoardStateDTO> getStartingBoardState() {
        return ResponseEntity.ok(gameService.initializeBoardState());
    }

    @PostMapping("/promote-to-piece")
    public ResponseEntity<BoardStateDTO> promoteToPiece(@RequestBody PromotionDTO promotionDTO) {
        return ResponseEntity.ok(gameService.promoteToPiece(promotionDTO));
    }
}
