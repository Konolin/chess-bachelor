package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.*;
import com.example.backend.utils.ChessUtils;

/**
 * This service handles the creation and parsing of FEN (Forsyth-Edwards Notation) strings.
 * It provides methods to create a game from a FEN string and to generate a FEN string from the current game state.
 */
public class FenService {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FenService() {
        throw new ChessException("not instantiable", ChessExceptionCodes.ILLEGAL_STATE);
    }

    /**
     * Converts a FEN string to a string representation of the board
     *
     * @param fen The FEN string to convert.
     * @return A string representation of the board.
     */
    public static String convertFENToStringBoard(String fen) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fen.length(); i++) {
            char c = fen.charAt(i);
            if (c == '/') {
                // new line
                sb.append('\n');
            } else if (Character.isDigit(c)) {
                // repeat the empty tile representation by the numeric value of c
                sb.append("- ".repeat(Math.max(0, Character.getNumericValue(c))));
            } else {
                sb.append(c).append(' ');
            }
        }

        return sb.toString();
    }

    /**
     * Creates a chess game (Board) from the given FEN string.
     *
     * @param fenString The FEN string representing the board state.
     * @return A Board object representing the game state described by the FEN string.
     * @throws ChessException If the FEN string is invalid.
     */
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
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.ROOK));
                    i++;
                    break;
                case 'n':
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.KNIGHT));
                    i++;
                    break;
                case 'b':
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.BISHOP));
                    i++;
                    break;
                case 'q':
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.QUEEN));
                    i++;
                    break;
                case 'k':
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.KING));
                    i++;
                    break;
                case 'p':
                    builder.setPieceAtPosition(new Piece(i, Alliance.BLACK, PieceType.PAWN));
                    if (!enPassantString.equals("-") &&
                            ChessUtils.getAlgebraicNotationAtCoordinate(i - 8).equals(enPassantString)) {
                        builder.setEnPassantPawnPosition(i);
                    }
                    i++;
                    break;
                case 'R':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.ROOK));
                    i++;
                    break;
                case 'N':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.KNIGHT));
                    i++;
                    break;
                case 'B':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.BISHOP));
                    i++;
                    break;
                case 'Q':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.QUEEN));
                    i++;
                    break;
                case 'K':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.KING));
                    i++;
                    break;
                case 'P':
                    builder.setPieceAtPosition(new Piece(i, Alliance.WHITE, PieceType.PAWN));
                    if (!enPassantString.equals("-") &&
                            ChessUtils.getAlgebraicNotationAtCoordinate(i + 8).equals(enPassantString)) {
                        builder.setEnPassantPawnPosition(i);
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

        builder.setCastleCapabilities(calculateCastleCapabilities(fenPartitions[2]));
        builder.setMoveMaker(moveMaker(fenPartitions[1]));
        return builder.build();
    }

    /**
     * Calculates the castling capabilities of the board based on the FEN string.
     *
     * @param castleString The castling rights portion of the FEN string.
     * @return An array of booleans representing the castling capabilities of the board.
     * The array is ordered as follows: [blackKingSide, blackQueenSide, whiteKingSide, whiteQueenSide].
     */
    private static boolean[] calculateCastleCapabilities(final String castleString) {
        return new boolean[]{
                castleString.contains("k"),
                castleString.contains("q"),
                castleString.contains("K"),
                castleString.contains("Q")
        };
    }

    /**
     * Generates a FEN string from the current state of the game (Board).
     *
     * @param board The current game state.
     * @return A FEN string representing the board state.
     */
    public static String createFENFromGame(final Board board) {
        return calculateBoardText(board) + " " +
                calculateCurrentPlayerText(board) + " " +
                calculateCastleText(board) + " " +
                calculateEnPassantText(board);
    }

    /**
     * Determines which player's turn it is based on the FEN string's second field (move maker).
     *
     * @param moveMakerString The move maker string ("w" or "b").
     * @return The alliance representing the player whose turn it is.
     * @throws ChessException If the move maker string is invalid.
     */
    private static Alliance moveMaker(final String moveMakerString) {
        if (moveMakerString.equals("w")) {
            return Alliance.WHITE;
        } else if (moveMakerString.equals("b")) {
            return Alliance.BLACK;
        }
        throw new ChessException("Invalid FEN String " + moveMakerString, ChessExceptionCodes.INVALID_FEN_STRING);
    }

    /**
     * Generates the board layout section of the FEN string from the current board state.
     *
     * @param board The current game state.
     * @return The board layout portion of the FEN string.
     */
    private static String calculateBoardText(final Board board) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            final String tileText = board.getPieceStringAtPosition(i);
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

    /**
     * Generates the current player section of the FEN string from the current board state.
     *
     * @param board The current game state.
     * @return The current player portion of the FEN string ("w" or "b").
     */
    private static String calculateCurrentPlayerText(final Board board) {
        return board.getMoveMaker().toString().substring(0, 1).toLowerCase();
    }

    /**
     * Generates the castling rights section of the FEN string from the current board state.
     *
     * @param board The current game state.
     * @return The castling rights portion of the FEN string (e.g., "KQkq").
     */
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

    /**
     * Generates the en passant target square section of the FEN string from the current board state.
     *
     * @param board The current game state.
     * @return The en passant target square (e.g., "e3" or "-").
     */
    private static String calculateEnPassantText(final Board board) {
        final int enPassantPawnPosition = board.getEnPassantPawnPosition();
        if (enPassantPawnPosition != -1) {
            // the position behind the pawn
            return ChessUtils.getAlgebraicNotationAtCoordinate(enPassantPawnPosition);
        }
        return "-";
    }
}
