package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.transpositionTable.NodeType;
import com.example.backend.models.transpositionTable.TranspositionEntry;
import com.example.backend.utils.MoveUtils;
import com.example.backend.utils.ZobristUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Alpha-Beta with Principal Variation Search (PVS).
 * Improvements over Move Ordering Alpha-Beta:
 * - Principal Variation Search for more efficient searching
 * - PV table to track and display the principal variation
 * - Aspiration windows for narrower searches after the first iteration
 * - Internal iterative deepening when TT move is not available
 */
public class MoveSearch {
    private static final Logger logger = LoggerFactory.getLogger(MoveSearch.class);

    // Constants
    private static final int MAX_DEPTH = 30;
    private static final int TT_SIZE = 1024 * 1024; // 1M entries
    private static final int MAX_KILLER_MOVES = 2;
    private static final int ASPIRATION_WINDOW = 25; // In centi-pawns

    // Time management
    private static final float TIME_BUFFER_FACTOR = 0.8f; // Stop earlier to account for overhead

    // Transposition table
    private static final TranspositionEntry[] transpositionTable = new TranspositionEntry[TT_SIZE];

    // Move ordering helpers
    private static final int[][] killerMoves = new int[MAX_DEPTH][MAX_KILLER_MOVES];
    private static final int[][] historyTable = new int[64][64]; // From-To square history

    // Principal Variation (PV) tracking
    private static final int[][] pvTable = new int[MAX_DEPTH][MAX_DEPTH];
    private static final int[] pvLength = new int[MAX_DEPTH];

    // Search statistics
    private static int nodesSearched = 0;
    private static int evaluations = 0;
    private static int ttHits = 0;
    private static int cutoffs = 0;
    private static int aspirationFailLows = 0;
    private static int aspirationFailHighs = 0;
    private static long startTime;
    private static long endTime;  // New: explicit end time tracking
    private static boolean stopSearch = false;

    /**
     * Find the best move for the current board position within time limit
     *
     * @param board  The current chess board state
     * @param timeMs Maximum search time in milliseconds
     * @return The best move found
     */
    public static int findBestMove(Board board, int timeMs) {
        // Initialize search state
        clearStatistics();
        startTime = System.currentTimeMillis();

        // Calculate end time with a buffer to ensure we don't overrun
        endTime = startTime + (long)(timeMs * TIME_BUFFER_FACTOR);

        stopSearch = false;

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

        // Iterative deepening framework
        int bestMove = rootMoves.get(0); // Always have a move to return
        float bestScore = Float.NEGATIVE_INFINITY;
        int completedDepth = 0;

        // Increase depth until time runs out or max depth reached
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            // Reset aspiration window for this depth
            float alpha = Float.NEGATIVE_INFINITY;
            float beta = Float.POSITIVE_INFINITY;
            float depthBestScore = Float.NEGATIVE_INFINITY;
            int depthBestMove = 0;
            int aspirationRetries = 0;

            // Use aspiration windows after the first depth is completed
            if (depth > 1) {
                alpha = bestScore - ASPIRATION_WINDOW / 100.0f;
                beta = bestScore + ASPIRATION_WINDOW / 100.0f;
            }

            // Track if we need to retry due to aspiration window failure
            boolean needRetry;

            do {
                // Check if we're already out of time before starting a new iteration
                if (System.currentTimeMillis() >= endTime) {
                    stopSearch = true;
                    break;
                }

                needRetry = false;

                // PV from previous iteration - these moves should be searched first
                if (depth > 1 && pvLength[0] > 0) {
                    orderRootMoves(rootMoves, board, pvTable[0][0]);
                } else {
                    // Just sort moves for the first iteration
                    orderMoves(rootMoves, board, 0, 0);
                }

                // Search all moves at the root
                for (int i = 0; i < rootMoves.size(); i++) {
                    int move = rootMoves.get(i);

                    // Execute the move
                    board.executeMove(move);

                    // For first move, always do a full search
                    float score;
                    if (i == 0) {
                        // Initialize PV tracking for this iteration
                        pvLength[0] = 0;

                        // Full window search
                        score = -alphaBeta(board, depth - 1, -beta, -alpha, 1, true);
                    }
                    // For remaining moves, try a null window search first
                    else {
                        // Null window search
                        score = -alphaBeta(board, depth - 1, -alpha - 0.01f, -alpha, 1, false);

                        // If the move might be better than alpha, do a full re-search
                        if (score > alpha && score < beta && !stopSearch) {
                            pvLength[0] = 0; // Reset PV for potential new best move
                            score = -alphaBeta(board, depth - 1, -beta, -alpha, 1, true);
                        }
                    }

                    // Undo the move
                    board.undoLastMove();

                    // Check if time is up
                    if (stopSearch) {
                        break;
                    }

                    logger.debug("Depth: {} Move: {} Score: {}", depth, move, score);

                    // Update best move if this move is better
                    if (score > depthBestScore) {
                        depthBestScore = score;
                        depthBestMove = move;

                        // Update alpha for pruning
                        if (score > alpha) {
                            alpha = score;

                            // Record this move as first in the PV
                            if (pvLength[0] > 0) {
                                pvTable[0][0] = move;
                            }
                        }
                    }
                }

                // If search was stopped, exit retry loop
                if (stopSearch) {
                    break;
                }

                // Check for aspiration window failures and adjust as needed
                if (depthBestScore <= alpha) {
                    // Score is too low - adjust alpha and retry
                    aspirationFailLows++;
                    aspirationRetries++;

                    // Progressively widen the window on repeated fails
                    if (aspirationRetries > 2) {
                        alpha = Float.NEGATIVE_INFINITY;
                        logger.debug("Depth {}: Multiple fail-lows, using full window", depth);
                    } else {
                        alpha = alpha - (ASPIRATION_WINDOW * aspirationRetries) / 100.0f;
                        logger.debug("Depth {}: Fail-low, retrying with alpha={}", depth, alpha);
                    }

                    needRetry = true;
                } else if (depthBestScore >= beta) {
                    // Score is too high - adjust beta and retry
                    aspirationFailHighs++;
                    aspirationRetries++;

                    // Progressively widen the window on repeated fails
                    if (aspirationRetries > 2) {
                        beta = Float.POSITIVE_INFINITY;
                        logger.debug("Depth {}: Multiple fail-highs, using full window", depth);
                    } else {
                        beta = beta + (ASPIRATION_WINDOW * aspirationRetries) / 100.0f;
                        logger.debug("Depth {}: Fail-high, retrying with beta={}", depth, beta);
                    }

                    needRetry = true;
                }

                // Exit loop if we've already found a mate or time is up
                if (stopSearch || Math.abs(depthBestScore) > 900) {
                    needRetry = false;
                }

            } while (needRetry);

            // If search was stopped, use best move from previous depth
            if (stopSearch) {
                logger.info("Search stopped during depth {}", depth);
                break;
            }

            // Update best move with the completed depth results
            bestMove = depthBestMove;
            bestScore = depthBestScore;
            completedDepth = depth;

            // Log progress with PV line
            logPVLine(depth, bestScore);

            // Check if time is nearly up - don't start a new depth if we might not finish
            if (System.currentTimeMillis() >= endTime) {
                logger.info("Time limit approaching, stopping at depth {}", completedDepth);
                stopSearch = true;
                break;
            }

            // If we found a checkmate, no need to search deeper
            if (bestScore > 900 || bestScore < -900) {
                logger.info("Mate found, stopping search");
                break;
            }
        }

        // Log search statistics
        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("Search completed at depth {} in {} ms", completedDepth, timeElapsed);
        logger.info("Nodes searched: {}", nodesSearched);
        logger.info("Evaluations: {}", evaluations);
        logger.info("TT hits: {}", ttHits);
        logger.info("Cutoffs: {}", cutoffs);
        logger.info("Aspiration window fails: {} low, {} high", aspirationFailLows, aspirationFailHighs);
        logger.info("Best move: {} (score: {})", MoveUtils.toAlgebraic(bestMove), bestScore);

        return bestMove;
    }

    /**
     * Alpha-beta search algorithm with PVS optimization and PV tracking
     */
    private static float alphaBeta(Board board, int depth, float alpha, float beta, int ply, boolean isPVNode) {
        // Update node counter
        nodesSearched++;

        // Check if search time is up - this is the fix!
        if (System.currentTimeMillis() >= endTime) {
            stopSearch = true;
            return 0; // Return a neutral value when stopping
        }

        // Initialize PV length
        pvLength[ply] = 0;

        // Recursion base case: leaf node
        if (depth <= 0) {
            return evaluate(board);
        }

        // Probe transposition table
        long zobristKey = ZobristUtils.computeZobristHash(board);
        TranspositionEntry ttEntry = probeTranspositionTable(zobristKey);

        if (!isPVNode && ttEntry != null && ttEntry.getDepth() >= depth) {
            ttHits++;

            // Use TT entry if it provides enough information for this node type
            switch (ttEntry.getNodeType()) {
                case EXACT:
                    // Store TT move in PV
                    if (ttEntry.getBestMove() != 0) {
                        pvTable[ply][0] = ttEntry.getBestMove();
                        pvLength[ply] = 1;
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
            alphaBeta(board, depth - 2, alpha, beta, ply, true);
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
        orderMoves(moves, board, ttMove, ply);

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
                score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, isPVNode);
            } else {
                // Try a null window search first for efficiency
                score = -alphaBeta(board, depth - 1, -alpha - 0.01f, -alpha, ply + 1, false);

                // If the move might be better than alpha, do a full re-search
                if (score > alpha && score < beta && isPVNode) {
                    score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, true);
                }
            }

            // Undo the move
            board.undoLastMove();

            // Stop search if time is up
            if (stopSearch) {
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
                    pvTable[ply][0] = move;
                    System.arraycopy(pvTable[ply + 1], 0, pvTable[ply], 1, pvLength[ply + 1]);
                    pvLength[ply] = pvLength[ply + 1] + 1;

                    // Alpha-beta pruning
                    if (alpha >= beta) {
                        cutoffs++;

                        // Update killer moves for non-captures
                        if (!MoveUtils.getMoveType(move).isAttack() && killerMoves[ply][0] != move) {
                            killerMoves[ply][1] = killerMoves[ply][0];
                            killerMoves[ply][0] = move;
                        }

                        // Update history heuristic
                        int from = MoveUtils.getFromTileIndex(move);
                        int to = MoveUtils.getToTileIndex(move);
                        historyTable[from][to] += depth * depth;

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

        for (int i = 0; i < pvLength[0]; i++) {
            sb.append(MoveUtils.toAlgebraic(pvTable[0][i])).append(" ");
        }

        logger.info(sb.toString());
    }

    /**
     * Special ordering for root moves - make sure the previous best move is first
     */
    private static void orderRootMoves(MoveList moves, Board board, int previousBest) {
        // First, normal ordering
        orderMoves(moves, board, previousBest, 0);

        // Then ensure the previous best move is first
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i) == previousBest) {
                // Swap with the first move
                if (i > 0) {
                    int temp = moves.get(0);
                    moves.set(0, previousBest);
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
    private static void orderMoves(MoveList moves, Board board, int ttMove, int ply) {
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
            else if (move == killerMoves[ply][0]) {
                score = 900000;
            } else if (move == killerMoves[ply][1]) {
                score = 800000;
            }
            // History heuristic
            else {
                int from = MoveUtils.getFromTileIndex(move);
                int to = MoveUtils.getToTileIndex(move);
                score = historyTable[from][to];
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
        evaluations++;

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
        nodesSearched = 0;
        evaluations = 0;
        ttHits = 0;
        cutoffs = 0;
        aspirationFailLows = 0;
        aspirationFailHighs = 0;

        // Clear PV table
        Arrays.fill(pvLength, 0);
    }

    /**
     * Clear all tables
     */
    public static void clearTables() {
        // Clear transposition table
        Arrays.fill(transpositionTable, null);

        // Clear killer moves
        for (int i = 0; i < MAX_DEPTH; i++) {
            Arrays.fill(killerMoves[i], 0);
        }

        // Clear history table
        for (int i = 0; i < 64; i++) {
            Arrays.fill(historyTable[i], 0);
        }

        clearStatistics();
    }

    /**
     * Example usage with time control
     */
    public static void main(String[] args) {
        // Create a board from FEN
        Board board = FenService.createGameFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        // Search for best move with 2 seconds
        int bestMove = MoveSearch.findBestMove(board, 2000);

        System.out.println("Best move: " + MoveUtils.toAlgebraic(bestMove));
    }
}