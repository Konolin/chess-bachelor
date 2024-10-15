package com.example.backend.controllers;

import com.example.backend.models.ChessUtils;
import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.dtos.AllMovesDTO;
import com.example.backend.models.dtos.BoardDTO;
import com.example.backend.services.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {
    @GetMapping("/starting-board")
    public ResponseEntity<BoardDTO> getStartingBoard() {
        return ResponseEntity.ok(GameService.getStartingBoard());
    }

    @GetMapping("/get-legal-moves-indexes")
    public ResponseEntity<AllMovesDTO> getLegalMovesIndexes(@RequestParam Integer tileIndex) {
        // TODO - temporary
        return ResponseEntity.ok(new AllMovesDTO(List.of(new Move(tileIndex, tileIndex + 8), new Move(tileIndex, tileIndex - 8)), List.of(new Move(tileIndex, tileIndex - 8))));
    }

    @GetMapping("make-move")
    public ResponseEntity<BoardDTO> makeMove(@RequestParam Integer fromTileIndex, @RequestParam Integer toTileIndex) {
        // TODO - temporary
        return ResponseEntity.ok(new BoardDTO("rnbqkbnr/ppppppPp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    }

    @GetMapping("test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok(Board.createStartingBoard().toString());
    }
}
