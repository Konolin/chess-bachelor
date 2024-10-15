package com.example.backend.models.player;

import com.example.backend.models.Alliance;
import com.example.backend.models.pieces.Piece;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Player {
    private final List<Piece> activePieces;

    Player(List<Piece> activePieces) {
        this.activePieces = activePieces;
    }

    public abstract Player getOpponent();

    public abstract Alliance getAlliance();
}
