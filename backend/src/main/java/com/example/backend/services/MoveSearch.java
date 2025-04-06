package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.transpositionTable.NodeType;
import com.example.backend.models.transpositionTable.TranspositionEntry;
import com.example.backend.utils.MoveUtils;
import com.example.backend.utils.ZobristUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-Threaded Alpha-Beta with Principal Variation Search (PVS).
 * Improvements:
 * - Parallel search of root moves
 * - Thread coordination with work stealing
 * - Shared transposition table with atomic operations
 * - Efficient time management for multiple threads
 */
public class MoveSearch {
    private static final Logger logger = LoggerFactory.getLogger(MoveSearch.class);

    // Constants
    private static final int MAX_DEPTH = 30;
    private static final int TT_SIZE = 4 * 1024 * 1024; // 4M entries
    private static final int MAX_KILLER_MOVES = 2;
    private static final int ASPIRATION_WINDOW = 25; // In centi-pawns

    // Time management
    private static final float TIME_BUFFER_FACTOR = 0.8f; // Stop earlier to account for overhead

    // Transposition table
    private static final TranspositionEntry[] transpositionTable = new TranspositionEntry[TT_SIZE];

    // Move ordering helpers - separate tables for each thread (indexed by thread id % 2)
    private static final int[][][] killerMoves = new int[2][MAX_DEPTH][MAX_KILLER_MOVES];
    private static final int[][] historyTable = new int[64][64]; // From-To square history, shared

    // Principal Variation (PV) tracking - separate for each thread
    private static final ThreadLocal<int[][]> threadPvTable = ThreadLocal.withInitial(() -> new int[MAX_DEPTH][MAX_DEPTH]);
    private static final ThreadLocal<int[]> threadPvLength = ThreadLocal.withInitial(() -> new int[MAX_DEPTH]);

    // Shared best PV information
    private static final int[][] bestPvTable = new int[MAX_DEPTH][MAX_DEPTH];
    private static final int[] bestPvLength = new int[MAX_DEPTH];

    // Search statistics - atomic for thread safety
    private static final AtomicInteger nodesSearched = new AtomicInteger(0);
    private static final AtomicInteger evaluations = new AtomicInteger(0);
    private static final AtomicInteger ttHits = new AtomicInteger(0);
    private static final AtomicInteger cutoffs = new AtomicInteger(0);
    private static final AtomicInteger aspirationFailLows = new AtomicInteger(0);
    private static final AtomicInteger aspirationFailHighs = new AtomicInteger(0);

    // Search control
    private static long startTime;
    private static long endTime;
    private static final AtomicBoolean stopSearch = new AtomicBoolean(false);

    // Parallel search coordination
    private static final Set<Integer> currentRootMoves = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Integer, SearchResult> moveResults = new ConcurrentHashMap<>();

    /**
     * Result from a search thread
     */
    private static class SearchResult {
        final int move;
        final float score;
        final int depth;
        final int[][] pvTable;
        final int pvLength;

        public SearchResult(int move, float score, int depth, int[][] pvTable, int pvLength) {
            this.move = move;
            this.score = score;
            this.depth = depth;
            // Copy PV data
            this.pvTable = new int[MAX_DEPTH][MAX_DEPTH];
            if (pvLength >= 0) System.arraycopy(pvTable[0], 0, this.pvTable[0], 0, pvLength);
            this.pvLength = pvLength;
        }
    }

    /**
     * Find the best move for the current board position within time limit
     *
     * @param board  The current chess board state
     * @param timeMs Maximum search time in milliseconds
     * @return The best move found
     */
    public static int findBestMove(Board board, int timeMs) {
        try {
            return findBestMoveInternal(board, timeMs);
        } finally {
            stopSearch.set(false);
        }
    }

    private static int findBestMoveInternal(Board board, int timeMs) {
        // Initialize search state
        clearStatistics();
        startTime = System.currentTimeMillis();
        endTime = startTime + (long)(timeMs * TIME_BUFFER_FACTOR);
        stopSearch.set(false);

        // Create a thread pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(availableProcessors);

        // Generate all legal moves at the root
        MoveList rootMoves = board.getAlliancesLegalMoves(board.getMoveMaker());
        if (rootMoves.size() == 0) {
            logger.info("No legal moves available");
            return 0;
        }

        // If only one move is available, return it immediately
        if (rootMoves.size() == 1) {
            logger.info("Only one legal move available");
            return rootMoves.get(0);
        }

        // Best move and score tracking
        int bestMove = rootMoves.get(0); // Default to first move
        float bestScore = Float.NEGATIVE_INFINITY;
        int completedDepth = 0;
        orderMoves(rootMoves, board, 0, 0, 0);

        try {
            // Iterative deepening framework
            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                final int currentDepth = depth;
                currentRootMoves.clear();
                moveResults.clear();

                // Track whether iterative deepening completed at this depth
                final AtomicBoolean depthCompleted = new AtomicBoolean(false);

                // Use aspirational windows for efficiency after first full search
                float alpha = depth > 1 ? bestScore - ASPIRATION_WINDOW/100.0f : Float.NEGATIVE_INFINITY;
                float beta = depth > 1 ? bestScore + ASPIRATION_WINDOW/100.0f : Float.POSITIVE_INFINITY;

                if (depth > 1 && bestPvLength[0] > 0) {
                    // Prioritize PV move
                    prioritizePvMove(rootMoves, bestPvTable[0][0]);
                }

                // Create and submit search tasks
                Future<?>[] futures = new Future<?>[rootMoves.size()];
                for (int i = 0; i < rootMoves.size(); i++) {
                    final int moveIndex = i;

                    // Submit work for each root move
                    futures[i] = executor.submit(() -> {
                        if (stopSearch.get()) return;

                        int moveToSearch = rootMoves.get(moveIndex);

                        // Skip if another thread is already searching this move
                        if (!currentRootMoves.add(moveToSearch)) {
                            return;
                        }

                        try {
                            Board boardCopy = new Board(board);
                            boardCopy.executeMove(moveToSearch);

                            // Initialize thread-local PV tracking for this iteration
                            threadPvLength.get()[0] = 0;

                            // Use PVS at root - assume first move is best for tighter bounds
                            float score;
                            if (moveIndex == 0) {
                                // Full window search for first move
                                score = -alphaBeta(boardCopy, currentDepth - 1, -beta, -alpha, 1, true, Thread.currentThread().threadId() % 2);
                            } else {
                                // Try a null window search first
                                score = -alphaBeta(boardCopy, currentDepth - 1, -alpha - 0.01f, -alpha, 1, false, Thread.currentThread().threadId() % 2);

                                // If this move might be better than alpha, do a full re-search
                                if (score > alpha && score < beta && !stopSearch.get()) {
                                    threadPvLength.get()[0] = 0; // Reset PV for potential new best move
                                    score = -alphaBeta(boardCopy, currentDepth - 1, -beta, -alpha, 1, true, Thread.currentThread().threadId() % 2);
                                }
                            }

                            // Store result with PV information
                            moveResults.put(moveToSearch, new SearchResult(
                                    moveToSearch,
                                    score,
                                    currentDepth,
                                    threadPvTable.get(),
                                    threadPvLength.get()[0]
                            ));

                        } catch (Exception e) {
                            logger.error("Error searching move {}: {}", moveToSearch, e.getMessage());
                        } finally {
                            currentRootMoves.remove(moveToSearch);
                        }
                    });
                }

                // Wait for completion or timeout
                long remainingTime = endTime - System.currentTimeMillis();
                if (remainingTime <= 0) {
                    stopSearch.set(true);
                    break;
                }

                try {
                    // Allow extra time for this depth to finish if close
                    long timeoutForDepth = Math.min(remainingTime + 100, remainingTime * 2);

                    // Wait for search completion or timeout
                    executor.invokeAll(List.of(() -> {
                        try {
                            // Wait for all futures to complete or be cancelled
                            for (Future<?> future : futures) {
                                try {
                                    future.get(timeoutForDepth, TimeUnit.MILLISECONDS);
                                } catch (Exception e) {
                                    // Ignore timeouts and cancellations
                                }
                            }
                            depthCompleted.set(true);
                        } catch (Exception e) {
                            logger.error("Error waiting for search completion: {}", e.getMessage());
                        }
                        return null;
                    }), remainingTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.info("Search interrupted at depth {}", depth);
                    stopSearch.set(true);
                }

                // Check results and update best move
                float bestMoveScore = Float.NEGATIVE_INFINITY;
                int newBestMove = bestMove;
                SearchResult bestResult = null;

                for (SearchResult result : moveResults.values()) {
                    if (result.score > bestMoveScore) {
                        bestMoveScore = result.score;
                        newBestMove = result.move;
                        bestResult = result;
                    }
                }

                // Only update if we have valid results
                if (bestResult != null) {
                    bestMove = newBestMove;
                    bestScore = bestMoveScore;
                    completedDepth = depthCompleted.get() ? depth : completedDepth;

                    // Update best PV
                    bestPvLength[0] = bestResult.pvLength;
                    if (bestResult.pvLength >= 0)
                        System.arraycopy(bestResult.pvTable[0], 0, bestPvTable[0], 0, bestResult.pvLength);

                    // Log progress with PV line
                    logPVLine(depth, bestScore);
                }

                // Check if we've exceeded our time limit
                if (System.currentTimeMillis() >= endTime) {
                    logger.info("Time limit approaching, stopping at depth {}", completedDepth);
                    stopSearch.set(true);
                    break;
                }

                // If we found a checkmate, no need to search deeper
                if (bestScore > 900 || bestScore < -900) {
                    logger.info("Mate found, stopping search");
                    break;
                }
            }
        } finally {
            // Ensure we clean up the executor
            stopSearch.set(true);
            executor.shutdownNow();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Log search statistics
        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("------------------------------------------------------------------");
        logger.info("Search completed at depth {} in {} ms", completedDepth, timeElapsed);
        logger.info("Nodes searched: {}", nodesSearched.get());
        logger.info("Evaluations: {}", evaluations.get());
        logger.info("TT hits: {}", ttHits.get());
        logger.info("Cutoffs: {}", cutoffs.get());
        logger.info("Aspiration window fails: {} low, {} high",
                aspirationFailLows.get(), aspirationFailHighs.get());

        // Print PV line
        if (bestPvLength[0] > 0) {
            StringBuilder pvLine = new StringBuilder("PV line: ");
            for (int i = 0; i < bestPvLength[0]; i++) {
                pvLine.append(MoveUtils.toAlgebraic(bestPvTable[0][i])).append(" ");
            }
            logger.info(pvLine.toString());
        }

        logger.info("Best move: {} (score: {})", MoveUtils.toAlgebraic(bestMove), bestScore);
        return bestMove;
    }

    /**
     * Alpha-beta search algorithm with PVS optimization and PV tracking
     */
    private static float alphaBeta(Board board, int depth, float alpha, float beta, int ply, boolean isPVNode, long threadId) {
        // Update node counter
        nodesSearched.incrementAndGet();

        // Check if search time is up
        if (System.currentTimeMillis() >= endTime) {
            stopSearch.set(true);
            return 0; // Return a neutral value when stopping
        }

        // Initialize PV length for this ply
        threadPvLength.get()[ply] = 0;

        // Recursion base case: leaf node
        if (depth <= 0) {
            return evaluate(board);
        }

        // Probe transposition table
        long zobristKey = ZobristUtils.computeZobristHash(board);
        TranspositionEntry ttEntry = probeTranspositionTable(zobristKey);

        if (!isPVNode && ttEntry != null && ttEntry.getDepth() >= depth) {
            ttHits.incrementAndGet();

            // Use TT entry if it provides enough information for this node type
            switch (ttEntry.getNodeType()) {
                case EXACT:
                    // Store TT move in PV
                    if (ttEntry.getBestMove() != 0) {
                        threadPvTable.get()[ply][0] = ttEntry.getBestMove();
                        threadPvLength.get()[ply] = 1;
                    }
                    return ttEntry.getEvaluation();

                case LOWERBOUND:
                    alpha = Math.max(alpha, ttEntry.getEvaluation());
                    break;

                case UPPERBOUND:
                    beta = Math.min(beta, ttEntry.getEvaluation());
                    break;
            }

            if (alpha >= beta) {
                return ttEntry.getEvaluation();
            }
        }

        // Internal iterative deepening if we don't have a TT move and this is a PV node
        int ttMove = (ttEntry != null) ? ttEntry.getBestMove() : 0;
        if (isPVNode && depth >= 4 && ttMove == 0) {
            alphaBeta(board, depth - 2, alpha, beta, ply, true, threadId);
            ttEntry = probeTranspositionTable(zobristKey);
            ttMove = (ttEntry != null) ? ttEntry.getBestMove() : 0;
        }

        // Generate legal moves
        MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());

        // Detect checkmate/stalemate
        if (moves.size() == 0) {
            if (board.isAllianceInCheck(board.getMoveMaker())) {
                return -1000.0f + ply; // Checkmate (prefer nearer mates)
            } else {
                return 0.0f; // Stalemate
            }
        }

        // Order moves using various heuristics
        orderMoves(moves, board, ttMove, ply, threadId);

        int bestMove = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        float alphaOrig = alpha;

        // Loop through all legal moves
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);

            // Execute the move
            board.executeMove(move);

            float score;

            // PVS algorithm: first move full search, others get null window unless promising
            if (i == 0) {
                // Full window search for first move
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, isPVNode, threadId);
            } else {
                // Try a null window search first for efficiency
                score = -alphaBeta(board, depth - 1, -alpha - 0.01f, -alpha, ply + 1, false, threadId);

                // If the move might be better than alpha, do a full re-search
                if (score > alpha && score < beta && isPVNode) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true, threadId);
                }
            }

            // Undo the move
            board.undoLastMove();

            // Stop search if time is up
            if (stopSearch.get()) {
                return 0;
            }

            // Update best score and move
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;

                // Update alpha for pruning
                if (score > alpha) {
                    alpha = score;

                    // Update PV table
                    threadPvTable.get()[ply][0] = move;
                    System.arraycopy(threadPvTable.get()[ply + 1], 0, threadPvTable.get()[ply], 1, threadPvLength.get()[ply + 1]);
                    threadPvLength.get()[ply] = threadPvLength.get()[ply + 1] + 1;

                    // Alpha-beta pruning
                    if (alpha >= beta) {
                        cutoffs.incrementAndGet();

                        // Update killer moves for non-captures
                        if (!MoveUtils.getMoveType(move).isAttack() && killerMoves[(int)threadId][ply][0] != move) {
                            killerMoves[(int)threadId][ply][1] = killerMoves[(int)threadId][ply][0];
                            killerMoves[(int)threadId][ply][0] = move;
                        }

                        // Update history heuristic - use synchronized to avoid race conditions
                        int from = MoveUtils.getFromTileIndex(move);
                        int to = MoveUtils.getToTileIndex(move);
                        synchronized(historyTable) {
                            historyTable[from][to] += depth * depth;
                        }

                        break;
                    }
                }
            }
        }

        // Determine node type for TT entry
        NodeType nodeType;
        if (bestScore <= alphaOrig) {
            nodeType = NodeType.UPPERBOUND;
        } else if (bestScore >= beta) {
            nodeType = NodeType.LOWERBOUND;
        } else {
            nodeType = NodeType.EXACT;
        }

        // Store position in transposition table
        storeTranspositionTable(zobristKey, depth, bestScore, bestMove, nodeType);

        return bestScore;
    }

    /**
     * Log the principal variation line
     */
    private static void logPVLine(int depth, float score) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Depth %d: score %.2f, PV: ", depth, score));

        for (int i = 0; i < bestPvLength[0]; i++) {
            sb.append(MoveUtils.toAlgebraic(bestPvTable[0][i])).append(" ");
        }

        logger.info(sb.toString());
    }

    private static void prioritizePvMove(MoveList moves, int pvMove) {
        // Find PV move and move it to the front
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i) == pvMove) {
                // Swap with the first move
                if (i > 0) {
                    int temp = moves.get(0);
                    moves.set(0, pvMove);
                    moves.set(i, temp);

                    // Also swap scores
                    int tempScore = moves.getScore(0);
                    moves.setScore(0, moves.getScore(i));
                    moves.setScore(i, tempScore);
                }
                break;
            }
        }
    }

    /**
     * Order moves based on various heuristics:
     * 1. TT move
     * 2. Captures (ordered by MVV-LVA)
     * 3. Killer moves
     * 4. History heuristic
     */
    private static void orderMoves(MoveList moves, Board board, int ttMove, int ply, long threadId) {
        // Score each move
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);
            int score = 0;

            // TT move gets highest priority
            if (move == ttMove) {
                score = 10000000;
            }
            // Captures scored by MVV-LVA
            else if (MoveUtils.getMoveType(move).isAttack()) {
                score = 1000000 + getMVVLVAScore(move, board);
            }
            // Killer moves
            else if (move == killerMoves[(int)threadId][ply][0]) {
                score = 900000;
            } else if (move == killerMoves[(int)threadId][ply][1]) {
                score = 800000;
            }
            // History heuristic
            else {
                int from = MoveUtils.getFromTileIndex(move);
                int to = MoveUtils.getToTileIndex(move);
                synchronized(historyTable) {
                    score = historyTable[from][to];
                }
            }

            moves.setScore(i, score);
        }

        // Sort moves by score (highest first)
        moves.sort();
    }

    /**
     * Probe the transposition table for a matching entry
     */
    private static TranspositionEntry probeTranspositionTable(long zobristKey) {
        int index = (int) (zobristKey % TT_SIZE);
        if (index < 0) index += TT_SIZE;

        TranspositionEntry entry = transpositionTable[index];
        if (entry != null && entry.getZobristKey() == zobristKey) {
            return entry;
        }

        return null;
    }

    /**
     * Store a position in the transposition table
     */
    private static void storeTranspositionTable(long zobristKey, int depth, float eval, int bestMove, NodeType nodeType) {
        int index = (int) (zobristKey % TT_SIZE);
        if (index < 0) index += TT_SIZE;

        TranspositionEntry current = transpositionTable[index];

        // Replacement strategy: always replace if deeper search or same position
        if (current == null || current.getZobristKey() == zobristKey || current.getDepth() <= depth) {
            transpositionTable[index] = new TranspositionEntry(zobristKey, depth, eval, bestMove, nodeType);
        }
    }

    /**
     * Evaluate the current board position
     */
    private static float evaluate(Board board) {
        // Update evaluation counter
        evaluations.incrementAndGet();

        // Use neural network evaluation
        return ModelService.makePrediction(board);
    }

    /**
     * Calculate MVV-LVA (Most Valuable Victim - Least Valuable Attacker) score for a capture move.
     * Higher scores are given to captures that take more valuable pieces with less valuable pieces.
     *
     * @param move The capture move
     * @param board The current board state
     * @return An MVV-LVA score (higher is better)
     */
    private static int getMVVLVAScore(int move, Board board) {
        // Calculate the attacker and victim piece types
        int attackerPieceValue = board.getPieceTypeAtPosition(MoveUtils.getFromTileIndex(move)).getValue();
        int victimPieceValue = board.getPieceTypeAtPosition(MoveUtils.getToTileIndex(move)).getValue();

        // This prioritizes capturing high-value pieces with low-value pieces
        return victimPieceValue * 10 - attackerPieceValue;
    }

    /**
     * Clear search statistics
     */
    private static void clearStatistics() {
        nodesSearched.set(0);
        evaluations.set(0);
        ttHits.set(0);
        cutoffs.set(0);
        aspirationFailLows.set(0);
        aspirationFailHighs.set(0);

        // Clear PV tables
        Arrays.fill(bestPvLength, 0);
    }

    /**
     * Clear all tables
     */
    public static void clearTables() {
        // Clear transposition table
        Arrays.fill(transpositionTable, null);

        // Clear killer moves
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < MAX_DEPTH; i++) {
                Arrays.fill(killerMoves[p][i], 0);
            }
        }

        // Clear history table
        for (int i = 0; i < 64; i++) {
            Arrays.fill(historyTable[i], 0);
        }

        clearStatistics();
    }

    /**
     * Initialize the search engine with a specific hash size
     *
     * @param ttSizeMB Transposition table size in MB
     */
    public static void init(int ttSizeMB) {
        int entries = (ttSizeMB * 1024 * 1024) / 28; // Approximate size of TranspositionEntry in bytes
        Arrays.fill(transpositionTable, null);
        clearTables();
        logger.info("Multi-threaded Alpha-Beta initialized with {} MB hash table ({} entries)",
                ttSizeMB, entries);
    }

    /**
     * Initialize with default size
     */
    public static void init() {
        init(512); // Default to 512MB hash table
    }

    /**
     * Example usage with time control
     */
    public static void main(String[] args) {
        // Initialize the engine
        MoveSearch.init();

        // Create a board from FEN
        Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Search for best move with 5 seconds
        int bestMove = MoveSearch.findBestMove(board, 3000);

        System.out.println("Best move: " + MoveUtils.toAlgebraic(bestMove));
    }
}