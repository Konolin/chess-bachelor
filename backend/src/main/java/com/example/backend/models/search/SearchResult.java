package com.example.backend.models.search;

import lombok.Getter;
import lombok.Setter;

import static com.example.backend.utils.MoveSearchConstants.MAX_DEPTH;

@Getter
@Setter
public class SearchResult {
    private final int move;
    private final float score;
    private final int depth;
    private final int[][] pvTable;
    private final int pvLength;

    public SearchResult(final int move, final float score, final int depth, final int[][] pvTable, final int pvLength) {
        this.move = move;
        this.score = score;
        this.depth = depth;
        this.pvTable = new int[MAX_DEPTH][MAX_DEPTH];
        if (pvLength >= 0) {
            System.arraycopy(pvTable[0], 0, this.pvTable[0], 0, pvLength);
        }
        this.pvLength = pvLength;
    }
}