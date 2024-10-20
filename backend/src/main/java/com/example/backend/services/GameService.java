package com.example.backend.services;

import com.example.backend.models.Move;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.Knight;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {
    public static List<Move> getAllLegalMoves(final Board board) {
        // TODO - temp
        return new Knight(0, Alliance.WHITE).generateLegalMoves();
    }
}
