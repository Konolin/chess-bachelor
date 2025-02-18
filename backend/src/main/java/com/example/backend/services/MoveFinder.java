package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.Move;

public class MoveFinder {
    public static Move findBestMove(Board board, int depth) {
        Move bestMove = null;
        float highestEvaluation = Float.MIN_VALUE;
        float lowestEvaluation = Float.MAX_VALUE;
        float currentEvaluation;

        for (final Move move : board.getAlliancesLegalMoves(board.getMoveMaker())) {
            Board candidateBoard = board.executeMove(move);
            currentEvaluation = board.getMoveMaker().isWhite()
                    ? min(candidateBoard, depth - 1, highestEvaluation, Float.MAX_VALUE)
                    : max(candidateBoard, depth - 1, Float.MIN_VALUE, lowestEvaluation);

            if (board.getMoveMaker().isWhite() && currentEvaluation >= highestEvaluation) {
                highestEvaluation = currentEvaluation;
                bestMove = move;
            } else if (board.getMoveMaker().isBlack() && currentEvaluation <= lowestEvaluation) {
                lowestEvaluation = currentEvaluation;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private static float min(final Board board, final int depth, final float alpha, float beta) {
        if (depth == 0 || board.isAllianceInCheckMate(board.getMoveMaker().getOpponent())) {
            return ModelService.makePrediction(FenService.createFENFromGame(board));
        }
        float lowestEvaluation = Float.MAX_VALUE;
        for (final Move move : board.getAlliancesLegalMoves(board.getMoveMaker())) {
            final Board candidateBoard = board.executeMove(move);

            final float currentEvaluation = max(candidateBoard, depth - 1, alpha, beta);
            if (currentEvaluation <= lowestEvaluation) {
                lowestEvaluation = currentEvaluation;
            }
            if (lowestEvaluation <= alpha) {
                return lowestEvaluation;
            }
            beta = Math.min(beta, lowestEvaluation);
        }
        return lowestEvaluation;
    }

    private static float max(final Board board, final int depth, final float alpha, float beta) {
        if (depth == 0 || board.isAllianceInCheckMate(board.getMoveMaker().getOpponent())) {
            return ModelService.makePrediction(FenService.createFENFromGame(board));
        }
        float highestEvaluation = Float.MIN_VALUE;
        for (final Move move : board.getAlliancesLegalMoves(board.getMoveMaker())) {
            final Board candidateBoard = board.executeMove(move);

            final float currentValue = min(candidateBoard, depth - 1, alpha, beta);
            if (currentValue >= highestEvaluation) {
                highestEvaluation = currentValue;
            }
            if (highestEvaluation >= beta) {
                return highestEvaluation;
            }
            beta = Math.max(beta, highestEvaluation);
        }
        return highestEvaluation;
    }

    public static void main(String[] args) {
        Board board = FenService.createGameFromFEN("bqrnnkrb/pppppppp/8/8/8/8/PPPPPPPP/BQRNNKRB w - - 0 1");
        System.out.println(findBestMove(board, 3).toAlgebraic());
    }
}
