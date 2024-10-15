package com.example.backend.models;

import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.Piece;
import lombok.Getter;

@Getter
public abstract class Move {
    private final Piece piece;
    private final int destinationPosition;
    private final boolean isFirstMove;
    private final Board board;

    protected Move(
            final Piece piece,
            final int destinationPosition,
            final Board board) {
        this.piece = piece;
        this.destinationPosition = destinationPosition;
        this.isFirstMove = this.piece.isFirstMove();
        this.board = board;
    }

    public int getStartingPosition() {
        return this.piece.getPosition();
    }

    protected Board.Builder placeTheOtherPieces(Board.Builder builder) {
        // place all pieces of the current player on the new board, except for the moved piece
        for (final Piece friendlyPiece : this.board.getCurrentPlayer().getActivePieces()) {
            if (!this.piece.equals(friendlyPiece)) {
                builder.setPieceAtPosition(friendlyPiece);
            }
        }
        // place all pieces of the opponent on the new board
        for (final Piece opposingPiece : this.board.getCurrentPlayer().getOpponent().getActivePieces()) {
            builder.setPieceAtPosition(opposingPiece);
        }
        return builder;
    }

    public Board execute() {
        final Board.Builder builder = placeTheOtherPieces(new Board.Builder());
        // set the moved piece and change the current move maker
        builder.setPieceAtPosition(this.piece.movePiece(this));
        builder.setMoveMaker(this.board.getCurrentPlayer().getOpponent().getAlliance());
        builder.setTransitionMove(this);
        // build the new board and return it
        return builder.build();
    }
}
