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
    private static final AtomicInteger uniqueEvaluations = new AtomicInteger(0);
    private static final AtomicInteger transpositionTableUses = new AtomicInteger(0);


    public static int findBestMove(Board board, int seconds) throws InterruptedException {
        transpositionTableUses.set(0);
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
            float score = alphaBeta(board, depth, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, endTime);

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

    private static float alphaBeta(Board board, int depth, float alpha, float beta, long endTime) {
        if (depth == 0 || board.isAllianceInCheckMate(board.getMoveMaker()) || System.currentTimeMillis() > endTime) {
            return evaluate(board);
        }

        float alphaOrig = alpha;
        long zobristKey = ZobristUtils.computeZobristHash(board);
        TranspositionEntry entry = transpositionTable.get(zobristKey);

        if (entry != null && entry.getDepth() >= depth) {
            transpositionTableUses.incrementAndGet();
            switch (entry.getNodeType()) {
                case EXACT: return entry.getEvaluation();
                case LOWERBOUND: alpha = Math.max(alpha, entry.getEvaluation()); break;
                case UPPERBOUND: beta = Math.min(beta, entry.getEvaluation()); break;
            }
            if (alpha >= beta) return entry.getEvaluation();
        }

        MoveList alliancesLegalMoves = board.getAlliancesLegalMoves(board.getMoveMaker());
        int ttMove = entry != null ? entry.getBestMove() : 0;
        if (ttMove != 0) {
            alliancesLegalMoves.moveToFront(ttMove);
        }

        float value = Float.NEGATIVE_INFINITY;
        int bestLocalMove = 0;

        for (int i = 0; i < alliancesLegalMoves.size(); i++) {
            int move = alliancesLegalMoves.get(i);
            board.executeMove(move);
            float score = -alphaBeta(board, depth - 1, -beta, -alpha, endTime);
            board.undoLastMove();

            if (score > value) {
                value = score;
                bestLocalMove = move;
            }

            alpha = Math.max(alpha, value);
            if (alpha >= beta) break;
        }

        NodeType type;
        if (value <= alphaOrig) type = NodeType.UPPERBOUND;
        else if (value >= beta) type = NodeType.LOWERBOUND;
        else type = NodeType.EXACT;

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
        System.out.println(MoveUtils.toAlgebraic(findBestMove(board, 20)));

        System.out.println("Time taken in seconds: " + (System.currentTimeMillis() - start) / 1000);
        System.out.println("Unique evaluations: " + uniqueEvaluations);
        System.out.println("Zobrist table uses: " + transpositionTableUses);
    }
}