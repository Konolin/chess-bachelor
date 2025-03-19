package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.dtos.BoardStateDTO;
import com.example.backend.models.moves.MoveList;
import com.example.backend.utils.MoveUtils;
import com.example.backend.utils.ZobristUtils;

import java.util.HashMap;
import java.util.Map;

public class MoveFinder {
    private static int evaulations = 0;
    private static int zobristKeyUses = 0;
    private static Map<Long, Float> transpositionTable = new HashMap<>();

    public static int alphaBetaSearch(Board board, int depth) {
        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        int bestMove = -1;

        MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());
        MoveUtils.sortMoveListByMoveScore(moves, board);

        if (board.getMoveMaker().isWhite()) {
            float bestVal = Float.NEGATIVE_INFINITY;

            for (int i = 0; i < moves.size(); i++) {
                board.executeMove(moves.get(i));

                float value = min(board, depth - 1, alpha, beta);

                board.undoLastMove();

                if (value > bestVal) {
                    bestVal = value;
                    bestMove = moves.get(i);
                }

                alpha = Math.max(alpha, bestVal);

                if (alpha >= beta) {
                    break;
                }
            }
        } else {
            float bestVal = Float.POSITIVE_INFINITY;

            for (int i = 0; i < moves.size(); i++) {
                board.executeMove(moves.get(i));

                float value = max(board, depth - 1, alpha, beta);

                board.undoLastMove();

                if (value < bestVal) {
                    bestVal = value;
                    bestMove = moves.get(i);
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

        MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());
        MoveUtils.sortMoveListByMoveScore(moves, board);

        for (int i = 0; i < moves.size(); i++) {
            board.executeMove(moves.get(i));

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

        MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());
        MoveUtils.sortMoveListByMoveScore(moves, board);

        for (int i = 0; i < moves.size(); i++) {
            board.executeMove(moves.get(i));

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

    private static float evaluate(Board board) {
        evaulations++;
        long zobristKey = ZobristUtils.computeZobristHash(board);
        if (transpositionTable.containsKey(zobristKey)) {
            zobristKeyUses++;
            return transpositionTable.get(zobristKey);
        }
        float prediction = ModelService.makePrediction(board);
        transpositionTable.put(zobristKey, prediction);
        return prediction;
    }

    public void main(String[] args) {
        Board board = FenService.createGameFromFEN("6k1/pp2Q1p1/2p4p/7r/8/6P1/Pq1r1P1P/4R1K1 w - - 0 1");
        long start = System.currentTimeMillis();
        System.out.println(MoveUtils.toAlgebraic(alphaBetaSearch(board, 4)));

        System.out.println("Time taken in seconds: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println("Evaluations: " + evaulations);
        System.out.println("Zobrist key uses: " + zobristKeyUses);
    }
}
