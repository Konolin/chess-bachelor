package com.example.backend.models.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The BoardStateDTO class is a Data Transfer Object (DTO) used to represent the state of a chessboard.
 * This class is intended for transferring the board's state information between layers or systems.
 */
@Getter
@Setter
@NoArgsConstructor
public class BoardStateDTO {

    /**
     * The FEN (Forsyth-Edwards Notation) string representing the current state of the chessboard.
     * FEN is a standard notation for describing the positions of pieces on a chessboard.
     */
    private String fen;

    /**
     * The winner flag indicating the result of the game:
     * 1 - White is the winner
     * 0 - Game is not finished
     * -1 - Black is the winner
     */
    private int winnerFlag;
}
