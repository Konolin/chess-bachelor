package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.dtos.BoardDTO;

public class GameService {
    private GameService() {
        throw new ChessException("Not instantiable", ExceptionCodes.ILLEGAL_STATE);
    }

    public static BoardDTO getStartingBoard() {
        return new BoardDTO(FENService.createFENFromGame(Board.createStartingBoard()));
    }
}
