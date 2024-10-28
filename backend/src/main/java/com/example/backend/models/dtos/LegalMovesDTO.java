package com.example.backend.models.dtos;

import com.example.backend.models.moves.Move;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LegalMovesDTO {
    private List<Move> legalMoves;
}
