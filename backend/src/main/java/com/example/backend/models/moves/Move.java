package com.example.backend.models.moves;

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

    @Override
    public String toString() {
        return fromTileIndex + " - " + toTileIndex + " ( " + moveType.name() + " ) ";
    }
}
