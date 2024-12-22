package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.utils.ChessUtils;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.*;

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
                    builder.setPieceAtPosition(new Pawn(i, Alliance.BLACK, ChessUtils.isPositionInRow(i, 2)));
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
                    builder.setPieceAtPosition(new Pawn(i, Alliance.WHITE, ChessUtils.isPositionInRow(i, 7)));
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
        final Pawn enPassantPawn = board.getEnPassantPawn();
        if (enPassantPawn != null) {
            // the position behind the pawn
            return ChessUtils.getAlgebraicNotationAtCoordinate(enPassantPawn.getPosition() +
                    8 * enPassantPawn.getAlliance().getOppositeDirection());
        }
        return "-";
    }

    /**
     * Determines if the white king can castle based on the FEN string's castling rights.
     *
     * @param fenCastleString The castling rights section from the FEN string.
     * @return True if the white king can castle, false otherwise.
     */
    private static boolean canWhiteKingCastle(final String fenCastleString) {
        return fenCastleString.contains("K") || fenCastleString.contains("Q");
    }

    /**
     * Determines if the black king can castle based on the FEN string's castling rights.
     *
     * @param fenCastleString The castling rights section from the FEN string.
     * @return True if the black king can castle, false otherwise.
     */
    private static boolean canBlackKingCastle(final String fenCastleString) {
        return fenCastleString.contains("k") || fenCastleString.contains("q");
    }
}
