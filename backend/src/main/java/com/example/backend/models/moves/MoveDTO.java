package com.example.backend.models.moves;

import com.example.backend.models.pieces.PieceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Move class represents a chess move from one tile to another.
 * It encapsulates information about the source and destination tiles, the type of move (e.g., regular move, castling, promotion),
 * and any special information related to the move (e.g., promoted piece type).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoveDTO {
    private int fromTileIndex;
    private int toTileIndex;
    private MoveType moveType;
    private PieceType promotedPieceType;
}
