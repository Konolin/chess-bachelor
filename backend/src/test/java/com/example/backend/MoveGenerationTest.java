package com.example.backend;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.services.FenService;
import com.example.backend.utils.MoveUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

// values used for testing from: https://www.chessprogramming.org/Perft_Results
class MoveGenerationTest {
    private final Logger logger = LoggerFactory.getLogger(MoveGenerationTest.class);

    long generateMovesCount(Board board, final long depth) {
        return generateFollowingMovesCount(board, depth, true, false);
    }

    long generateFollowingMovesCount(Board board, final long depth, boolean isFirstIteration, boolean isDebug) {
        if (depth == 1) {
            return board.getAlliancesLegalMoves(board.getMoveMaker()).size();
        }

        MoveList legalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());

        long numMoves = 0;
        for (int i = 0; i < legalMoves.size(); i++) {
            board.executeMove(legalMoves.get(i));

            long followingMoves = generateFollowingMovesCount(board, depth - 1, false, false);
            if (isFirstIteration && isDebug) {
                logger.info("{}: {}", MoveUtils.toAlgebraic(legalMoves.get(i)), followingMoves);
            }
            numMoves += followingMoves;

            board.undoLastMove();
        }

        return numMoves;
    }

    @Test
    void testMoveGenerationPosition1() {
        Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -");
        long numMoves;
        int[] expectedMoves = {20, 400, 8_902, 197_281, 4_865_609, 119_060_324};
        for (int i = 0; i < expectedMoves.length; i++) {
            numMoves = generateMovesCount(board, i + 1);
            assertEquals(expectedMoves[i], numMoves);
        }
    }

    @Test
    void testMoveGenerationPosition2() {
        Board board = FenService.createGameFromFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        long numMoves;
        int[] expectedMoves = {48, 2039, 97_862, 4_085_603, 193_690_690};
        for (int i = 0; i < expectedMoves.length; i++) {
            numMoves = generateMovesCount(board, i + 1);
            assertEquals(expectedMoves[i], numMoves);
        }
    }

    @Test
    void testMoveGenerationPosition3() {
        Board board = FenService.createGameFromFEN("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - ");
        long numMoves;
        int[] expectedMoves = {14, 191, 2_812, 43_238, 674_624, 11_030_083, 178_633_661};
        for (int i = 0; i < expectedMoves.length; i++) {
            numMoves = generateMovesCount(board, i + 1);
            assertEquals(expectedMoves[i], numMoves);
        }
    }

    @Test
    void testMoveGenerationPosition4() {
        Board board = FenService.createGameFromFEN("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -");
        long numMoves;

        int[] expectedMoves = {6, 264, 9467, 422333, 15833292, 706045033};
        for (int i = 1; i <= 6; i++) {
            numMoves = generateMovesCount(board, i);
            assertEquals(expectedMoves[i - 1], numMoves);
        }
    }

    @Test
    void testMoveGenerationPosition5() {
        Board board = FenService.createGameFromFEN("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -");
        long numMoves;
        int[] expectedMoves = {44, 1_486, 62_379, 2_103_487, 89_941_194};
        for (int i = 0; i < expectedMoves.length; i++) {
            numMoves = generateMovesCount(board, i + 1);
            assertEquals(expectedMoves[i], numMoves);
        }
    }

    @Test
    void testMoveGenerationPosition6() {
        Board board = FenService.createGameFromFEN("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -");
        long numMoves;
        int[] expectedMoves = {46, 2_079, 89_890, 3_894_594, 164_075_551};
        for (int i = 0; i < expectedMoves.length; i++) {
            numMoves = generateMovesCount(board, i + 1);
            assertEquals(expectedMoves[i], numMoves);
        }
    }
}
