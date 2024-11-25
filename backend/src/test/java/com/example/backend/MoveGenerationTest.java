package com.example.backend;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.services.ChessValidator;
import com.example.backend.services.FenService;
import com.example.backend.services.GameService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// values used for testing from: https://www.chessprogramming.org/Perft_Results
class MoveGenerationTest {
    private final Logger logger = LoggerFactory.getLogger(MoveGenerationTest.class);
    private final GameService gameService = new GameService(new ChessValidator());

    long generateMovesCount(Board board, final long depth) {
        List<Move> legalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());

        long numMoves = 0;
        for (final Move move : legalMoves) {
            Board newBoard = board.executeMove(move);
            gameService.setBoard(newBoard);

            long followingMoves = generateFollowingMovesCount(newBoard, depth - 1);
//            logger.info("{}: {}", move.toAlgebraic(), followingMoves);
            numMoves += followingMoves;

            gameService.setBoard(board);
        }

        return numMoves;
    }

    long generateFollowingMovesCount(Board board, final long depth) {
        if (depth == 0) {
            return 1;
        }

        List<Move> legalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());

        long numMoves = 0;
        for (final Move move : legalMoves) {
            Board newBoard = board.executeMove(move);
            gameService.setBoard(newBoard);
            numMoves += generateFollowingMovesCount(newBoard, depth - 1);
            gameService.setBoard(board);
        }

        return numMoves;
    }


    @Test
    void testMoveGenerationPosition1() {
        Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(20, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(400, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(8902, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(197281, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 5);
        assertEquals(4865609, numMoves);
    }

    @Test
    void testMoveGenerationPosition2() {
        Board board = FenService.createGameFromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(48, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(2039, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(97862, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(4085603, numMoves);
    }

    @Test
    void testMoveGenerationPosition3() {
        Board board = FenService.createGameFromFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - ");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(14, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(191, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(2812, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(43238, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 5);
        assertEquals(674624, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 6);
        assertEquals(11030083, numMoves);
    }

    @Test
    void  testMoveGenerationPosition4() {
        Board board = FenService.createGameFromFEN("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(6, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(264, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(9467, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(422333, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 5);
        assertEquals(15833292, numMoves);
    }

    @Test
    void testMoveGenerationPosition5() {
        Board board = FenService.createGameFromFEN("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(44, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(1486, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(62379, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(2103487, numMoves);
    }

    @Test
    void testMoveGenerationPosition6() {
        Board board = FenService.createGameFromFEN("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 1);
        assertEquals(46, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 2);
        assertEquals(2079, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 3);
        assertEquals(89890, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesCount(board, 4);
        assertEquals(3894594, numMoves);
    }
}
