package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.search.SearchResult;
import com.example.backend.models.transpositionTable.NodeType;
import com.example.backend.models.transpositionTable.TranspositionEntry;
import com.example.backend.utils.MoveUtils;
import com.example.backend.utils.ZobristUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.backend.utils.MoveSearchConstants.*;

/**
 * Multi-Threaded Alpha-Beta with Principal Variation Search (PVS).
 * Improvements:
 * - Parallel search of root moves
 * - Thread coordination with work stealing
 * - Shared transposition table with atomic operations
 * - Efficient time management for multiple threads
 */
public class MoveSearch {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoveSearch.class);

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
    private static final AtomicBoolean stopSearch = new AtomicBoolean(false);
    // Parallel search coordination
    private static final Set<Integer> currentRootMoves = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Integer, SearchResult> moveResults = new ConcurrentHashMap<>();
    // Search control
    private static long startTime;
    private static long endTime;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPvTable.remove();
            threadPvLength.remove();
        }));
    }

    /**
     * Find the best move for the current board position within time limit
     *
     * @param board  The current chess board state
     * @param timeMs Maximum search time in milliseconds
     * @return The best move found
     */
    public static int findBestMove(final Board board, final int timeMs) {
        try {
            return findBestMoveInternal(board, timeMs);
        } finally {
            stopSearch.set(false);
        }
    }

    private static void initializeSearchParameters(final int timeMs) {
        clearStatistics();
        startTime = System.currentTimeMillis();
        endTime = startTime + (long) (timeMs * TIME_BUFFER_FACTOR);
        stopSearch.set(false);
    }

    private static ExecutorService createSearchThreadPool() {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(availableProcessors);
    }

    private static MoveList initializeAndValidateRootMoves(final Board board) {
        final MoveList rootMoves = board.getAlliancesLegalMoves(board.getMoveMaker());
        if (rootMoves.isEmpty()) {
            LOGGER.info("No legal moves available");
            return null;
        }
        if (rootMoves.size() == 1) {
            LOGGER.info("Only one legal move available");
            return rootMoves;
        }
        orderMoves(rootMoves, board, 0, 0, 0);
        return rootMoves;
    }

    private static void logSearchStatistics(final long startTime, final int completedDepth, final int bestMove, final float bestScore) {
        final long timeElapsed = System.currentTimeMillis() - startTime;
        LOGGER.info("------------------------------------------------------------------");
        LOGGER.info("Search completed at depth {} in {} ms", completedDepth, timeElapsed);
        LOGGER.info("Nodes searched: {}", nodesSearched.get());
        LOGGER.info("Evaluations: {}", evaluations.get());
        LOGGER.info("TT hits: {}", ttHits.get());
        LOGGER.info("Cutoffs: {}", cutoffs.get());
        LOGGER.info("Aspiration window fails: {} low, {} high", aspirationFailLows.get(), aspirationFailHighs.get());
        if (bestPvLength[0] > 0) {
            final StringBuilder pvLine = new StringBuilder("PV line: ");
            for (int i = 0; i < bestPvLength[0]; i++) {
                pvLine.append(MoveUtils.toAlgebraic(bestPvTable[0][i])).append(" ");
            }
            LOGGER.info(pvLine.toString());
        }
        LOGGER.info("Best move: {} (score: {})", MoveUtils.toAlgebraic(bestMove), bestScore);
    }

    private static boolean shouldTerminateSearch(final int depth, final float bestScore) {
        if (System.currentTimeMillis() >= endTime) {
            LOGGER.info("Time limit approaching, stopping at depth {}", depth);
            stopSearch.set(true);
            return true;
        }
        if (bestScore > 900 || bestScore < -900) {
            LOGGER.info("Mate found, stopping search");
            return true;
        }
        return false;
    }

    private static int findBestMoveInternal(final Board board, final int timeMs) {
        initializeSearchParameters(timeMs);
        final ExecutorService executor = createSearchThreadPool();

        try {
            final MoveList rootMoves = initializeAndValidateRootMoves(board);
            if (rootMoves == null) {
                return 0;
            }

            int bestMove = rootMoves.get(0);
            float bestScore = Float.NEGATIVE_INFINITY;
            int completedDepth = 0;

            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                currentRootMoves.clear();
                moveResults.clear();

                // track whether iterative deepening completed at this depth
                final AtomicBoolean depthCompleted = new AtomicBoolean(false);

                // use aspirational windows for efficiency after first full search
                final float alpha = depth > 1 ? bestScore - ASPIRATION_WINDOW / 100.0f : Float.NEGATIVE_INFINITY;
                final float beta = depth > 1 ? bestScore + ASPIRATION_WINDOW / 100.0f : Float.POSITIVE_INFINITY;

                if (depth > 1 && bestPvLength[0] > 0) {
                    rootMoves.prioritizePvMove(bestPvTable[0][0]);
                }

                // create and submit search tasks
                final Future<?>[] futures = new Future<?>[rootMoves.size()];
                for (int i = 0; i < rootMoves.size(); i++) {
                    // submit work for each root move
                    futures[i] = executor.submit(
                            createSearchTask(board, rootMoves, i, depth, alpha, beta)
                    );
                }

                // wait for completion or timeout
                final long remainingTime = endTime - System.currentTimeMillis();
                if (remainingTime <= 0) {
                    stopSearch.set(true);
                    break;
                }

                waitForFuturesCompletion(futures, remainingTime, depthCompleted);

                // check results and update best move
                float bestMoveScore = Float.NEGATIVE_INFINITY;
                int newBestMove = bestMove;
                SearchResult bestResult = null;

                for (final SearchResult result : moveResults.values()) {
                    if (result.getScore() > bestMoveScore) {
                        bestMoveScore = result.getScore();
                        newBestMove = result.getMove();
                        bestResult = result;
                    }
                }

                // only update if we have valid results
                if (bestResult != null) {
                    bestMove = newBestMove;
                    bestScore = bestMoveScore;
                    completedDepth = depthCompleted.get() ? depth : completedDepth;

                    updatePrincipalVariation(
                            bestResult.getPvTable(), new int[]{bestResult.getPvLength()}, bestPvTable, bestPvLength, 0
                    );

                    logPVLine(depth, bestScore);
                }

                if (shouldTerminateSearch(completedDepth, bestScore)) {
                    break;
                }
            }

            logSearchStatistics(startTime, completedDepth, bestMove, bestScore);
            return bestMove;
        } finally {
            stopSearch.set(true);
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.warn("Executor did not terminate in the specified time.");
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        LOGGER.error("Executor did not terminate after shutdownNow.");
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Thread was interrupted while waiting for executor termination.", e);
            }
        }
    }

    private static void waitForFuturesCompletion(final Future<?>[] futures, final long remainingTime, final AtomicBoolean depthCompleted) {
        try {
            // Calculate a flexible timeout to allow depth to complete
            final long timeoutForDepth = Math.min(remainingTime + 100, remainingTime * 2);

            // Wait for futures to complete
            for (final Future<?> future : futures) {
                try {
                    future.get(timeoutForDepth, TimeUnit.MILLISECONDS);
                } catch (final TimeoutException e) {
                    // Ignore timeout, continue waiting for other futures
                    LOGGER.debug("Future timed out: {}", e.getMessage());
                } catch (final InterruptedException e) {
                    // Restore interrupt status and stop search
                    Thread.currentThread().interrupt();
                    LOGGER.warn("Search interrupted while waiting for futures");
                    stopSearch.set(true);
                    break;
                } catch (final ExecutionException e) {
                    // Log any execution errors
                    LOGGER.error("Error in search task", e.getCause());
                }
            }

            // Mark depth as completed if we made it through all futures
            depthCompleted.set(true);
        } catch (final Exception e) {
            LOGGER.warn("Unexpected error in futures completion: {}", e.getMessage());
            stopSearch.set(true);
        }
    }

    private static float alphaBeta(final Board board, final int depth, float alpha, final float beta, final int ply,
                                   final boolean isPVNode, final long threadId) {
        nodesSearched.incrementAndGet();

        // check if search time is up
        if (System.currentTimeMillis() >= endTime) {
            stopSearch.set(true);
            return 0; // return a neutral value when stopping
        }

        // initialize PV length for this ply
        threadPvLength.get()[ply] = 0;

        // base case: leaf node
        if (depth <= 0) {
            return evaluate(board);
        }

        final long zobristKey = ZobristUtils.computeZobristHash(board);
        final Float cachedEval = lookupTranspositionTable(zobristKey, depth, alpha, beta, isPVNode, ply);
        if (cachedEval != null) {
            return cachedEval;
        }

        // internal iterative deepening for PV nodes
        final int ttMove = findTTMove(board, zobristKey, depth, isPVNode, ply, threadId);

        final MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());

        // detect checkmate/stalemate
        // if no legal moves and in check, it's checkmate (prefer nearer mates), else stalemate
        if (moves.isEmpty()) {
            return board.isAllianceInCheck(board.getMoveMaker()) ? -1000.0f - ply : 0.0f;
        }

        orderMoves(moves, board, ttMove, ply, threadId);

        int bestMove = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        final float alphaOrig = alpha;

        for (int i = 0; i < moves.size(); i++) {
            final int move = moves.get(i);

            board.executeMove(move);
            final float score = searchMove(board, i, depth, alpha, beta, ply, isPVNode, threadId);
            board.undoLastMove();

            // stop search if time is up
            if (stopSearch.get()) {
                return 0;
            }

            // update best score and move
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;

                // update alpha for pruning
                if (score > alpha) {
                    alpha = score;
                    updatePrincipalVariation(
                            threadPvTable.get(), threadPvLength.get(), threadPvTable.get(), threadPvLength.get(), move
                    );

                    // alpha-beta pruning
                    if (alpha >= beta) {
                        cutoffs.incrementAndGet();
                        updateMoveOrderingHeuristics(move, depth, ply, threadId);
                        break;
                    }
                }
            }
        }

        // determine and store node type in transposition table
        final NodeType nodeType = determineNodeType(bestScore, alphaOrig, beta);
        storeTranspositionTable(zobristKey, depth, bestScore, bestMove, nodeType);

        return bestScore;
    }

    private static Float lookupTranspositionTable(final long zobristKey, final int depth, float alpha, float beta,
                                                  final boolean isPVNode, final int ply) {
        final TranspositionEntry ttEntry = probeTranspositionTable(zobristKey);
        if (!isPVNode && ttEntry != null && ttEntry.getDepth() >= depth) {
            ttHits.incrementAndGet();
            switch (ttEntry.getNodeType()) {
                case EXACT -> {
                    if (ttEntry.getBestMove() != 0) {
                        threadPvTable.get()[ply][0] = ttEntry.getBestMove();
                        threadPvLength.get()[ply] = 1;
                    }
                    return ttEntry.getEvaluation();
                }
                case LOWERBOUND -> alpha = Math.max(alpha, ttEntry.getEvaluation());
                case UPPERBOUND -> beta = Math.min(beta, ttEntry.getEvaluation());
            }
            if (alpha >= beta) {
                return ttEntry.getEvaluation();
            }
        }
        return null;
    }

    /**
     * Find move from transposition table, potentially using internal iterative deepening
     */
    private static int findTTMove(final Board board, final long zobristKey, final int depth, final boolean isPVNode,
                                  final int ply, final long threadId) {
        TranspositionEntry ttEntry = probeTranspositionTable(zobristKey);
        int ttMove = (ttEntry != null) ? ttEntry.getBestMove() : 0;
        if (isPVNode && depth >= 4 && ttMove == 0) {
            alphaBeta(board, depth - 2, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, ply, true, threadId);
            ttEntry = probeTranspositionTable(zobristKey);
            ttMove = (ttEntry != null) ? ttEntry.getBestMove() : 0;
        }
        return ttMove;
    }

    /**
     * Search a single move with Principal Variation Search
     */
    private static float searchMove(final Board board, final int moveIndex, final int depth, final float alpha,
                                    final float beta, final int ply, final boolean isPVNode, final long threadId) {
        float score;
        if (moveIndex == 0) {
            // full window search for first move
            score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, isPVNode, threadId);
        } else {
            // try a null window search first
            score = -alphaBeta(board, depth - 1, -alpha - 0.01f, -alpha, ply + 1, false, threadId);
            // if promising, do a full re-search
            if (score > alpha && score < beta && isPVNode) {
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true, threadId);
            }
        }
        return score;
    }

    /**
     * Update Principal Variation table
     *
     * @param sourcePv       Source PV table
     * @param sourcePvLength Source PV length
     * @param targetPv       Target PV table
     * @param targetPvLength Target PV length
     * @param move           Move to add at the beginning (optional)
     */
    private static void updatePrincipalVariation(final int[][] sourcePv, final int[] sourcePvLength,
                                                 final int[][] targetPv, final int[] targetPvLength, final int move) {
        // if no move is provided, copy entire PV
        if (move == 0 && sourcePvLength[0] >= 0) {
            targetPvLength[0] = Math.min(sourcePvLength[0], targetPv[0].length);
            System.arraycopy(
                    sourcePv[0], 0,
                    targetPv[0], 0,
                    targetPvLength[0]
            );
            return;
        }

        // if move is provided, insert it at the beginning
        targetPv[0][0] = move;
        if (sourcePvLength[0] > 0) {
            // Make sure we don't exceed array bounds
            final int copyLength = Math.min(sourcePvLength[0], targetPv[0].length - 1);
            System.arraycopy(
                    sourcePv[0], 0,
                    targetPv[0], 1,
                    copyLength
            );
            targetPvLength[0] = copyLength + 1;
        } else {
            targetPvLength[0] = 1;
        }
    }

    /**
     * Update move ordering heuristics after a cutoff
     *
     * @param move     Best move
     * @param depth    Current search depth
     * @param ply      Current search depth
     * @param threadId Thread identifier
     */
    private static void updateMoveOrderingHeuristics(final int move, final int depth, final int ply, final long threadId) {
        // update killer moves for non-captures
        if (!MoveUtils.getMoveType(move).isAttack() &&
                killerMoves[(int) threadId][ply][0] != move) {
            killerMoves[(int) threadId][ply][1] = killerMoves[(int) threadId][ply][0];
            killerMoves[(int) threadId][ply][0] = move;
        }

        // update history heuristic
        final int from = MoveUtils.getFromTileIndex(move);
        final int to = MoveUtils.getToTileIndex(move);
        synchronized (historyTable) {
            historyTable[from][to] += depth * depth;
        }
    }

    /**
     * Determine the node type for transposition table storage
     *
     * @param bestScore Best score found
     * @param alphaOrig Original alpha value
     * @param beta      Beta value
     * @return Node type
     */
    private static NodeType determineNodeType(final float bestScore, final float alphaOrig, final float beta) {
        if (bestScore <= alphaOrig) {
            return NodeType.UPPERBOUND;
        }
        if (bestScore >= beta) {
            return NodeType.LOWERBOUND;
        }
        return NodeType.EXACT;
    }

    private static void logPVLine(final int depth, final float score) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Depth %d: score %.2f, PV: ", depth, score));
        for (int i = 0; i < bestPvLength[0]; i++) {
            sb.append(MoveUtils.toAlgebraic(bestPvTable[0][i])).append(" ");
        }
        LOGGER.info(sb.toString());
    }

    /**
     * Order moves based on various heuristics:
     * <ul>
     *     <li>TT move gets highest priority</li>
     *     <li>Captures are scored by MVV-LVA</li>
     *     <li>Killer moves are prioritized</li>
     *     <li>History heuristic is used for non-captures</li>
     * </ul>
     */
    private static void orderMoves(final MoveList moves, final Board board, final int ttMove, final int ply, final long threadId) {
        // score each move
        for (int i = 0; i < moves.size(); i++) {
            final int move = moves.get(i);
            final int score;
            // TT move gets highest priority
            if (move == ttMove) {
                score = 10000000;
            }
            // captures scored by MVV-LVA
            else if (MoveUtils.getMoveType(move).isAttack()) {
                score = 1000000 + getMVVLVAScore(move, board);
            }
            // killer moves
            else if (move == killerMoves[(int) threadId][ply][0]) {
                score = 900000;
            } else if (move == killerMoves[(int) threadId][ply][1]) {
                score = 800000;
            }
            // history heuristic
            else {
                final int from = MoveUtils.getFromTileIndex(move);
                final int to = MoveUtils.getToTileIndex(move);
                synchronized (historyTable) {
                    score = historyTable[from][to];
                }
            }
            moves.setScore(i, score);
        }
        // sort moves by score
        moves.sort();
    }

    private static TranspositionEntry probeTranspositionTable(final long zobristKey) {
        int index = (int) (zobristKey % TT_SIZE);
        if (index < 0) {
            index += TT_SIZE;
        }

        final TranspositionEntry entry = transpositionTable[index];

        if (entry != null && entry.getZobristKey() == zobristKey) {
            return entry;
        }
        return null;
    }

    private static void storeTranspositionTable(final long zobristKey, final int depth, final float eval,
                                                final int bestMove, final NodeType nodeType) {
        int index = (int) (zobristKey % TT_SIZE);
        if (index < 0) {
            index += TT_SIZE;
        }

        final TranspositionEntry current = transpositionTable[index];

        // replacement strategy: always replace if deeper search or same position
        // (bigger depth is better because it means more information is available for the position)
        if (current == null || current.getZobristKey() == zobristKey || current.getDepth() <= depth) {
            transpositionTable[index] = new TranspositionEntry(zobristKey, depth, eval, bestMove, nodeType);
        }
    }

    private static float evaluate(final Board board) {
        evaluations.incrementAndGet();
        return ModelService.makePrediction(board);
    }

    private static int getMVVLVAScore(final int move, final Board board) {
        final int attackerPieceValue = board.getPieceTypeAtPosition(MoveUtils.getFromTileIndex(move)).getValue();
        final int victimPieceValue = board.getPieceTypeAtPosition(MoveUtils.getToTileIndex(move)).getValue();
        // prioritize capturing high-value pieces with low-value pieces
        return victimPieceValue * 10 - attackerPieceValue;
    }

    private static void clearStatistics() {
        nodesSearched.set(0);
        evaluations.set(0);
        ttHits.set(0);
        cutoffs.set(0);
        aspirationFailLows.set(0);
        aspirationFailHighs.set(0);
        Arrays.fill(bestPvLength, 0);
    }

    private static void clearTables() {
        Arrays.fill(transpositionTable, null);
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < MAX_DEPTH; i++) {
                Arrays.fill(killerMoves[p][i], 0);
            }
        }
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
    public static void init(final int ttSizeMB) {
        final int entries = (ttSizeMB * 1024 * 1024) / 28; // approximate size of TranspositionEntry in bytes
        clearTables();
        LOGGER.info("Multi-threaded Alpha-Beta initialized with {} MB hash table ({} entries)", ttSizeMB, entries);
    }

    private static Callable<Void> createSearchTask(final Board board, final MoveList rootMoves, final int moveIndex,
                                                   final int currentDepth, final float alpha, final float beta) {
        return () -> {
            if (stopSearch.get()) {
                return null;
            }

            final int moveToSearch = rootMoves.get(moveIndex);

            // skip if another thread is already searching this move
            if (!currentRootMoves.add(moveToSearch)) {
                return null;
            }

            try {
                final Board boardCopy = new Board(board);
                boardCopy.executeMove(moveToSearch);

                // initialize thread-local PV tracking for this iteration
                threadPvLength.get()[0] = 0;

                final float score = performRootMoveSearch(boardCopy, moveIndex, currentDepth, alpha, beta);

                // store result with PV information
                moveResults.put(moveToSearch, new SearchResult(
                        moveToSearch, score, currentDepth, threadPvTable.get(), threadPvLength.get()[0]
                ));
            } catch (final Exception e) {
                LOGGER.error("Error searching move {}: {}", moveToSearch, e.getMessage());
            } finally {
                currentRootMoves.remove(moveToSearch);
            }
            return null;
        };
    }

    private static float performRootMoveSearch(final Board boardCopy, final int moveIndex, final int currentDepth,
                                               final float alpha, final float beta) {
        float score;
        if (moveIndex == 0) {
            // full window search for first move
            score = -alphaBeta(
                    boardCopy, currentDepth - 1, -beta, -alpha, 1, true, Thread.currentThread().threadId() % 2
            );
        } else {
            // try a null window search first
            score = -alphaBeta(
                    boardCopy, currentDepth - 1, -alpha - 0.01f, -alpha, 1, false, Thread.currentThread().threadId() % 2
            );
            // if this move might be better than alpha, do a full re-search
            if (score > alpha && score < beta && !stopSearch.get()) {
                threadPvLength.get()[0] = 0; // Reset PV for potential new best move
                score = -alphaBeta(
                        boardCopy, currentDepth - 1, -beta, -alpha, 1, true, Thread.currentThread().threadId() % 2
                );
            }
        }
        return score;
    }

    public static void main(final String[] args) {
        MoveSearch.init(512);
        final Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        final int bestMove = MoveSearch.findBestMove(board, 5000);
        LOGGER.info("Best move: {}", MoveUtils.toAlgebraic(bestMove));
    }
}