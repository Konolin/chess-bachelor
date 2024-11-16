package com.example.backend.models.moves;

import com.example.backend.models.pieces.Piece;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Move {
    private int fromTileIndex;
    private int toTileIndex;
    private MoveType moveType;
    private String promotedPieceChar;

    public Move(int fromTileIndex, int toTileIndex, MoveType moveType) {
        this.fromTileIndex = fromTileIndex;
        this.toTileIndex = toTileIndex;
        this.moveType = moveType;
        this.promotedPieceChar = null;
    }

    @Override
    public String toString() {
        return fromTileIndex + " - " + toTileIndex + " ( " + moveType.name() + " ) ";
    }
}
