package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;

import java.util.List;

public class MoveFinder {
    private static int evaulations = 0;

    public static Move findBestMove(Board board, int depth) {
        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        Move bestMove = null;

        List<Move> moves = board.getAlliancesLegalMoves(board.getMoveMaker());

        if (board.getMoveMaker().isWhite()) {
            float bestVal = Float.NEGATIVE_INFINITY;

            for (Move move : moves) {
                board.executeMove(move);

                float value = min(board, depth - 1, alpha, beta);

                board.undoLastMove();

                if (value > bestVal) {
                    bestVal = value;
                    bestMove = move;
                }

                alpha = Math.max(alpha, bestVal);

                if (alpha >= beta) {
                    break;
                }
            }
        } else {
            float bestVal = Float.POSITIVE_INFINITY;

            for (Move move : moves) {
                board.executeMove(move);

                float value = max(board, depth - 1, alpha, beta);

                board.undoLastMove();

                if (value < bestVal) {
                    bestVal = value;
                    bestMove = move;
                }

                beta = Math.min(beta, bestVal);

                if (beta <= alpha) {
                    break;
                }
            }
        }
        return bestMove;
    }

    private static float max(Board board, int depth, float alpha, float beta) {
        if (depth == 0
                || board.isAllianceInCheckMate(board.getMoveMaker())
                || board.isAllianceInStalemate(board.getMoveMaker())) {
            return evaluate(board);
        }

        float bestValue = Float.NEGATIVE_INFINITY;

        List<Move> moves = board.getAlliancesLegalMoves(board.getMoveMaker());
        for (Move move : moves) {
            board.executeMove(move);

            float value = min(board, depth - 1, alpha, beta);

            board.undoLastMove();

            bestValue = Math.max(bestValue, value);
            alpha = Math.max(alpha, bestValue);

            if (alpha >= beta) {
                break;
            }
        }
        return bestValue;
    }

    private static float min(Board board, int depth, float alpha, float beta) {
        if (depth == 0
                || board.isAllianceInCheckMate(board.getMoveMaker())
                || board.isAllianceInStalemate(board.getMoveMaker())) {
            return evaluate(board);
        }

        float bestValue = Float.POSITIVE_INFINITY;

        List<Move> moves = board.getAlliancesLegalMoves(board.getMoveMaker());
        for (Move move : moves) {
            board.executeMove(move);

            float value = max(board, depth - 1, alpha, beta);

            board.undoLastMove();

            bestValue = Math.min(bestValue, value);
            beta = Math.min(beta, bestValue);

            if (beta <= alpha) {
                break;
            }
        }
        return bestValue;
    }

    /**
     * Evaluate the board state using a neural network or heuristic
     *
     * @param board the board to evaluate
     * @return the evaluation of the board, a float
     */
    private static float evaluate(Board board) {
        evaulations++;
        return ModelService.makePrediction(FenService.createFENFromGame(board));
    }

    public static void main(String[] args) {
        Board board = FenService.createGameFromFEN("6k1/pp2Q1p1/2p4p/7r/8/6P1/Pq1r1P1P/4R1K1 w - - 0 1");
        System.out.println(findBestMove(board, 3).toAlgebraic());
        System.out.println("Evaluations: " + evaulations);
    }
}
