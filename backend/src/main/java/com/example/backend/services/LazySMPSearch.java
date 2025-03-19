package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.transpositionTable.NodeType;
import com.example.backend.models.transpositionTable.TranspositionEntry;
import com.example.backend.utils.MoveUtils;
import com.example.backend.utils.ZobristUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LazySMPSearch {
    private static final ConcurrentHashMap<Long, TranspositionEntry> transpositionTable = new ConcurrentHashMap<>();
    private static final AtomicInteger bestMove = new AtomicInteger(0);
    private static final AtomicReference<Float> bestEvaluation = new AtomicReference<>(Float.NEGATIVE_INFINITY);
    private static final AtomicInteger evaluations = new AtomicInteger(0);
    private static final AtomicInteger uniqueEvaluations = new AtomicInteger(0);


    public static int findBestMove(Board board, int seconds) throws InterruptedException {
        evaluations.set(0);
        uniqueEvaluations.set(0);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

        long endTime = System.currentTimeMillis() + seconds * 1000;

        for (int i = 0; i < availableProcessors; i++) {
            executor.submit(() -> iterativeDeepening(new Board(board), endTime));
        }

        executor.shutdown();
        executor.awaitTermination(seconds + 1, TimeUnit.SECONDS);

        return bestMove.get();
    }

    private static void iterativeDeepening(Board board, long endTime) {
        int depth = 1;
        while (System.currentTimeMillis() < endTime) {
            float score = alphaBeta(board, depth, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

            TranspositionEntry entry = transpositionTable.get(ZobristUtils.computeZobristHash(board));
            if (entry != null && entry.getBestMove() != 0) {
                updateBestMove(score, entry.getBestMove());
            }
            depth++;
        }
    }

    private static void updateBestMove(float score, int move) {
        synchronized (bestEvaluation) {
            if (score > bestEvaluation.get()) {
                bestEvaluation.set(score);
                bestMove.set(move);
            }
        }
    }

    private static float alphaBeta(Board board, int depth, float alpha, float beta) {
        if (depth == 0 || board.isAllianceInCheckMate(board.getMoveMaker())) {
            evaluations.incrementAndGet();
            return evaluate(board);
        }

        long zobristKey = ZobristUtils.computeZobristHash(board);
        TranspositionEntry entry = transpositionTable.get(zobristKey);
        if (entry != null && entry.getDepth() >= depth) {
            switch (entry.getNodeType()) {
                case NodeType.EXACT: return entry.getEvaluation();
                case NodeType.LOWERBOUND: alpha = Math.max(alpha, entry.getEvaluation()); break;
                case NodeType.UPPERBOUND: beta = Math.min(beta, entry.getEvaluation()); break;
            }
            if (alpha >= beta) return entry.getEvaluation();
        }

        float value = Float.NEGATIVE_INFINITY;
        int bestLocalMove = 0;
        MoveList alliancesLegalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());
        for (int i = 0; i < alliancesLegalMoves.size(); i++) {
            board.executeMove(alliancesLegalMoves.get(i));
            float score = -alphaBeta(board, depth - 1, -beta, -alpha);
            board.undoLastMove();

            if (score > value) {
                value = score;
                bestLocalMove = alliancesLegalMoves.get(i);
            }
            alpha = Math.max(alpha, value);
            if (alpha >= beta) break;
        }

        NodeType type = NodeType.EXACT;
        if (value <= alpha) type = NodeType.UPPERBOUND;
        else if (value >= beta) type = NodeType.LOWERBOUND;

        TranspositionEntry newEntry = new TranspositionEntry(depth, value, bestLocalMove, type);

        transpositionTable.put(zobristKey, newEntry);

        return value;
    }

    private static float evaluate(Board board) {
        uniqueEvaluations.incrementAndGet();
        return ModelService.makePrediction(board);
    }

    public static void main(String[] args) throws InterruptedException {
        Board board = FenService.createGameFromFEN("6k1/pp2Q1p1/2p4p/7r/8/6P1/Pq1r1P1P/4R1K1 w - - 0 1");
        long start = System.currentTimeMillis();
        System.out.println(MoveUtils.toAlgebraic(findBestMove(board, 10)));

        System.out.println("Time taken in seconds: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println("Evaluations: " + evaluations);
        System.out.println("Unique evaluations: " + uniqueEvaluations);
    }
}
