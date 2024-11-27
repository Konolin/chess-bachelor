package com.example.backend.models.dtos;

import com.example.backend.models.bitboards.BitBoards;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardStateDTO {
    private String fen;
    // 1 white is the winner; 0 game not finished; -1 black is the winner
    private int winnerFlag;
    private BitBoards bitBoards;
}
