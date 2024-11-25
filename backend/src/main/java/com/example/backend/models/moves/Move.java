package com.example.backend.models.moves;

import com.example.backend.utils.ChessUtils;
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

    public String toAlgebraic() {
        StringBuilder sb = new StringBuilder();

        if (moveType.isCastleMove()) {
            sb.append(moveType.isKingSideCastle() ? "O-O" : "O-O-O");
        } else {
            if (!moveType.isPromotion()) {
                sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(fromTileIndex));
            }
            sb.append(ChessUtils.getAlgebraicNotationAtCoordinate(toTileIndex));

            if (moveType.isPromotion()) {
                sb.append("=").append(promotedPieceChar.toUpperCase());
            }
        }

        return sb.toString();
    }
}
