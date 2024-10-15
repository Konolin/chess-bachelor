package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ExceptionCodes;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;

public class FENService {
    private FENService() {
        throw new ChessException("Not instantiable", ExceptionCodes.ILLEGAL_STATE);
    }

    // TODO - add method to create game from FEN string

    public static String createFENFromGame(final Board board) {
        return calculateBoardText(board);
        // TODO - add rest of fen string
    }

    private static String calculateBoardText(final Board board) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < ChessUtils.NUM_TILES; i++) {
            final String tileText = board.getTileAtPosition(i).toString();
            builder.append(tileText);
        }

        builder.insert(8, "/");
        builder.insert(17, "/");
        builder.insert(26, "/");
        builder.insert(35, "/");
        builder.insert(44, "/");
        builder.insert(53, "/");
        builder.insert(62, "/");

        return builder.toString()
                .replace("--------", "8")
                .replace("-------", "7")
                .replace("------", "6")
                .replace("-----", "5")
                .replace("----", "4")
                .replace("---", "3")
                .replace("--", "2")
                .replace("-", "1");
    }
}
