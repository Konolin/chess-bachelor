package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import com.example.backend.models.transpositionTable.NodeType;
import com.example.backend.models.transpositionTable.TranspositionEntry;
import com.example.backend.utils.ZobristUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Alpha-Beta search with Transposition Table.
 *
 * Improvements over Iterative Deepening Alpha-Beta:
 * - Transposition table to avoid researching identical positions
 * - Different node types (exact, lower bound, upper bound) for accurate pruning
 * - Best move storage for better move ordering
 */
public class LazySMPSearch {
    private static final Logger logger = LoggerFactory.getLogger(LazySMPSearch.class);

    // Constants
    private static final int MAX_DEPTH = 30;
    private static final int TT_SIZE = 1024 * 1024; // 1M entries

    // Transposition table
    private static final TranspositionEntry[] transpositionTable = new TranspositionEntry[TT_SIZE];

    // Search statistics
    private static int nodesSearched = 0;
    private static int evaluations = 0;
    private static int ttHits = 0;
    private static long startTime;
    private static boolean stopSearch = false;

    /**
     * Find the best move for the current board position within time limit
     *
     * @param board The current chess board state
     * @param timeMs Maximum search time in milliseconds
     * @return The best move found
     */
    public static int findBestMove(Board board, int timeMs) {
        // Initialize search state
        clearStatistics();
        startTime = System.currentTimeMillis();
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
            // Reset search parameters for each depth
            float alpha = Float.NEGATIVE_INFINITY;
            float beta = Float.POSITIVE_INFINITY;
            float depthBestScore = Float.NEGATIVE_INFINITY;
            int depthBestMove = 0;

            // Search all moves at the root
            for (int i = 0; i < rootMoves.size(); i++) {
                int move = rootMoves.get(i);

                // Execute the move
                board.executeMove(move);

                // Search the resulting position
                float score = -alphaBeta(board, depth - 1, -beta, -alpha, 0);

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
                    }
                }
            }

            // If we completed this depth without stopping, update the best move
            if (!stopSearch) {
                bestMove = depthBestMove;
                bestScore = depthBestScore;
                completedDepth = depth;

                logger.info("Depth {} completed: best move {} (score: {})",
                        depth, bestMove, bestScore);
            } else {
                break; // Stop iterative deepening if time is up
            }

            // Check if search time is up
            if (System.currentTimeMillis() - startTime >= timeMs) {
                stopSearch = true;
                break;
            }

            // If we found a checkmate, no need to search deeper
            if (bestScore > 900 || bestScore < -900) {
                break;
            }
        }

        // Log search statistics
        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("Search completed at depth {} in {} ms", completedDepth, timeElapsed);
        logger.info("Nodes searched: {}", nodesSearched);
        logger.info("Evaluations: {}", evaluations);
        logger.info("TT hits: {}", ttHits);
        logger.info("Best move: {} (score: {})", bestMove, bestScore);

        return bestMove;
    }

    /**
     * Alpha-beta search algorithm with transposition table
     */
    private static float alphaBeta(Board board, int depth, float alpha, float beta, int ply) {
        // Update node counter
        nodesSearched++;

        // Check if search time is up
        if (System.currentTimeMillis() - startTime >= startTime + 2000) {
            stopSearch = true;
            return 0; // Return a neutral value when stopping
        }

        // Recursion base case: leaf node
        if (depth <= 0) {
            return evaluate(board);
        }

        // Probe transposition table
        long zobristKey = ZobristUtils.computeZobristHash(board);
        TranspositionEntry ttEntry = probeTranspositionTable(zobristKey);

        if (ttEntry != null && ttEntry.getDepth() >= depth) {
            ttHits++;

            // Use TT entry if it provides enough information for this node type
            switch (ttEntry.getNodeType()) {
                case EXACT:
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

        // Try the TT move first, if available
        if (ttEntry != null && ttEntry.getBestMove() != 0) {
            int ttMove = ttEntry.getBestMove();

            // Check if TT move is in our legal moves
            for (int i = 0; i < moves.size(); i++) {
                if (moves.get(i) == ttMove) {
                    // Swap this move to the front for search
                    int temp = moves.get(0);
                    moves.set(0, ttMove);
                    moves.set(i, temp);
                    break;
                }
            }
        }

        int bestMove = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        float alphaOrig = alpha;

        // Loop through all legal moves
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);

            // Execute the move
            board.executeMove(move);

            // Recursively search the resulting position
            float score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);

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

                    // Alpha-beta pruning
                    if (alpha >= beta) {
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

        TranspositionEntry entry = new TranspositionEntry(zobristKey, depth, eval, bestMove, nodeType);
        transpositionTable[index] = entry;
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
     * Clear search statistics and tables
     */
    private static void clearStatistics() {
        nodesSearched = 0;
        evaluations = 0;
        ttHits = 0;
    }

    /**
     * Clear transposition table
     */
    public static void clearTranspositionTable() {
        Arrays.fill(transpositionTable, null);
    }

    /**
     * Example usage with time control
     */
    public static void main(String[] args) {
        // Create a board from FEN
        Board board = FenService.createGameFromFEN("r3k2r/ppp2ppp/2n2q2/3Np3/2B5/8/PPP2PPP/R1BQ2K1 w kq - 0 1");

        // Search for best move with 2 seconds
        int bestMove = LazySMPSearch.findBestMove(board, 5000);

        System.out.println("Best move: " + bestMove);
    }
}