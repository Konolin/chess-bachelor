package com.example.backend.models.transpositionTable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TranspositionEntry {
    private long zobristKey;
    private final int depth;
    private final float evaluation;
    private final int bestMove;
    private final NodeType nodeType;
}
