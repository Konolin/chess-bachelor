package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.*;

public class FenService {

    private FenService() {
        throw new ChessException("not instantiable", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static Board createGameFromFEN(final String fenString) {
        final String[] fenPartitions = fenString.trim().split(" ");
        final Board.Builder builder = new Board.Builder();
        final String enPassantString = fenPartitions[3];
        final String gameConfiguration = fenPartitions[0];

        final char[] boardTiles = gameConfiguration
                .replace("/", "")
                .replace("8", "--------")
                .replace("7", "-------")
                .replace("6", "------")
                .replace("5", "-----")
                .replace("4", "----")
                .replace("3", "---")
                .replace("2", "--")
                .replace("1", "-")
                .toCharArray();

        int i = 0;
        while (i < boardTiles.length) {
            switch (boardTiles[i]) {
                case 'r':
                    builder.setPieceAtPosition(new Rook(i, Alliance.BLACK, true));
                    i++;
                    break;
                case 'n':
                    builder.setPieceAtPosition(new Knight(i, Alliance.BLACK));
                    i++;
                    break;
                case 'b':
                    builder.setPieceAtPosition(new Bishop(i, Alliance.BLACK));
                    i++;
                    break;
                case 'q':
                    builder.setPieceAtPosition(new Queen(i, Alliance.BLACK));
                    i++;
                    break;
                case 'k':
                    builder.setPieceAtPosition(new King(i, Alliance.BLACK, canBlackKingCastle(fenPartitions[2])));
                    i++;
                    break;
                case 'p':
                    builder.setPieceAtPosition(new Pawn(i, Alliance.BLACK, ChessUtils.SECOND_ROW[i]));
                    if (!enPassantString.equals("-") &&
                            ChessUtils.getAlgebraicNotationAtCoordinate(i - 8).equals(enPassantString)) {
                        builder.setEnPassantPawn(new Pawn(i, Alliance.BLACK, false));
                    }
                    i++;
                    break;
                case 'R':
                    builder.setPieceAtPosition(new Rook(i, Alliance.WHITE, true));
                    i++;
                    break;
                case 'N':
                    builder.setPieceAtPosition(new Knight(i, Alliance.WHITE));
                    i++;
                    break;
                case 'B':
                    builder.setPieceAtPosition(new Bishop(i, Alliance.WHITE));
                    i++;
                    break;
                case 'Q':
                    builder.setPieceAtPosition(new Queen(i, Alliance.WHITE));
                    i++;
                    break;
                case 'K':
                    builder.setPieceAtPosition(new King(i, Alliance.WHITE, canWhiteKingCastle(fenPartitions[2])));
                    i++;
                    break;
                case 'P':
                    builder.setPieceAtPosition(new Pawn(i, Alliance.WHITE, ChessUtils.SEVENTH_ROW[i]));
                    if (!enPassantString.equals("-") &&
                            ChessUtils.getAlgebraicNotationAtCoordinate(i + 8).equals(enPassantString)) {
                        builder.setEnPassantPawn(new Pawn(i, Alliance.WHITE, false));
                    }
                    i++;
                    break;
                case '-':
                    i++;
                    break;
                default:
                    throw new ChessException("Invalid FEN String " + gameConfiguration, ChessExceptionCodes.INVALID_FEN_STRING);
            }
        }
        builder.setMoveMaker(moveMaker(fenPartitions[1]));
        return builder.build();
    }

    public static String createFENFromGame(final Board board) {
        return calculateBoardText(board) + " " +
                calculateCurrentPlayerText(board) + " " +
                calculateCastleText(board) + " " +
                calculateEnPassantText(board);
    }

    private static Alliance moveMaker(final String moveMakerString) {
        if (moveMakerString.equals("w")) {
            return Alliance.WHITE;
        } else if (moveMakerString.equals("b")) {
            return Alliance.BLACK;
        }
        throw new ChessException("Invalid FEN String " + moveMakerString, ChessExceptionCodes.INVALID_FEN_STRING);
    }

    private static String calculateBoardText(final Board board) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            final String tileText = board.getTileAtCoordinate(i).toString();
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

    private static String calculateCurrentPlayerText(final Board board) {
        return board.getMoveMaker().toString().substring(0, 1).toLowerCase();
    }

    private static String calculateCastleText(final Board board) {
        StringBuilder builder = new StringBuilder();
        if (board.isBlackKingSideCastleCapable()) {
            builder.append("k");
        }
        if (board.isBlackQueenSideCastleCapable()) {
            builder.append("q");
        }
        if (board.isWhiteKingSideCastleCapable()) {
            builder.append("K");
        }
        if (board.isWhiteQueenSideCastleCapable()) {
            builder.append("Q");
        }
        final String result = builder.toString();
        return result.isEmpty() ? "-" : result;
    }

    private static String calculateEnPassantText(final Board board) {
        final Pawn enPassantPawn = board.getEnPassantPawn();
        if (enPassantPawn != null) {
            // the position behind the pawn
            return ChessUtils.getAlgebraicNotationAtCoordinate(enPassantPawn.getPosition() +
                    8 * enPassantPawn.getAlliance().getOppositeDirection());
        }
        return "-";
    }

    private static boolean canWhiteKingCastle(final String fenCastleString) {
        return fenCastleString.contains("K") || fenCastleString.contains("Q");
    }

    private static boolean canBlackKingCastle(final String fenCastleString) {
        return fenCastleString.contains("k") || fenCastleString.contains("q");
    }
}
