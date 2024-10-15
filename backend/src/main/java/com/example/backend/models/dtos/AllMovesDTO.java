package com.example.backend.models.dtos;

import com.example.backend.models.Move;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllMovesDTO {
    private List<Move> allMoves;
    private List<Move> attackMoves;
}
