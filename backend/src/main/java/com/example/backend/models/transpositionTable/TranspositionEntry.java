package com.example.backend.models.transpositionTable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TranspositionEntry {
    private final int depth;
    private final float evaluation;
    private final int bestMove;
    private final NodeType nodeType;
}
