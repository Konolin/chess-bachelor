package com.example.backend.controllers;

import com.example.backend.models.BoardState;
import com.example.backend.models.ChessConstants;
import com.example.backend.models.Move;
import com.example.backend.models.dtos.AllMovesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<AllMovesDTO> getLegalMovesIndexes(@RequestParam Integer tileIndex) {
        // TODO - temporary
        return ResponseEntity.ok(new AllMovesDTO(List.of(new Move(tileIndex, tileIndex + 8), new Move(tileIndex, tileIndex - 8)), List.of(new Move(tileIndex, tileIndex - 8))));
    }

    @GetMapping("make-move")
    public ResponseEntity<BoardState> makeMove(@RequestParam Integer fromTileIndex, @RequestParam Integer toTileIndex) {
        // TODO - temporary
        return ResponseEntity.ok(new BoardState("rnbqkbnr/ppppppPp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    }
}
