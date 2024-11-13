package com.example.backend;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;
import com.example.backend.services.ChessValidator;
import com.example.backend.services.FenService;
import com.example.backend.services.GameService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoveGenerationTest {
    private final Logger logger = LoggerFactory.getLogger(MoveGenerationTest.class);
    private final GameService gameService = new GameService(new ChessValidator());

    public long generateMovesTest(Board board, final long depth) {
        if (depth == 0) {
            return 1;
        }

        Iterable<Move> legalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());
        long numMoves = 0;

        for (final Move move : legalMoves) {
            logger.info("Move: {}", move);
            Board newBoard = gameService.executeMove(move);
            gameService.setBoard(newBoard);
            numMoves += generateMovesTest(newBoard,depth - 1);
            gameService.setBoard(board);
        }

        return numMoves;
    }

    @Test
     void testMoveGenerationStandardBoard() {
        Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 1);
        assertEquals(20, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board,2);
        assertEquals(400, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board,3);
        assertEquals(8902, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board,4);
        assertEquals(197281, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board,5);
        assertEquals(4865609, numMoves);
    }

    @Test // https://www.chessprogramming.org/Perft_Results
     void testMoveGenerationPosition2() {
         Board board = FenService.createGameFromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
         long numMoves;

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 1);
         assertEquals(48, numMoves);

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 2);
         assertEquals(2039, numMoves);

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 3);
         assertEquals(97862, numMoves);

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 4);
         assertEquals(4085603, numMoves);

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 5);
         assertEquals(193690690, numMoves);

         gameService.setBoard(board);
         numMoves = generateMovesTest(board, 6);
         assertEquals(8031647685L, numMoves);
    }

    @Test
     void testMoveGenerationPosition3() {
        Board board = FenService.createGameFromFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - ");
        long numMoves;

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 1);
        assertEquals(14, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 2);
        assertEquals(191, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 3);
        assertEquals(2812, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 4);
        assertEquals(43238, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 5);
        assertEquals(674624, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 6);
        assertEquals(11030083, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 7);
        assertEquals(178633661, numMoves);

        gameService.setBoard(board);
        numMoves = generateMovesTest(board, 8);
        assertEquals(3009794393L, numMoves);
    }
}
