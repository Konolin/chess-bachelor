package com.example.backend.models;

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

    @Override
    public String toString() {
        return fromTileIndex + " - " + toTileIndex;
    }
}
