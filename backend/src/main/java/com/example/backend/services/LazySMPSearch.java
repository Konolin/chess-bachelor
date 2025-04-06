package com.example.backend.services;

import com.example.backend.models.board.Board;
import com.example.backend.models.moves.MoveList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic Alpha-Beta search implementation for a chess engine.
 * This is the foundation that we'll build upon with various optimizations.
 */
public class LazySMPSearch {
    private static final Logger logger = LoggerFactory.getLogger(LazySMPSearch.class);

    // Search statistics
    private static int nodesSearched = 0;
    private static int evaluations = 0;
    private static long startTime;

    /**
     * Find the best move for the current board position with a fixed depth
     *
     * @param board The current chess board state
     * @param depth Search depth
     * @return The best move found
     */
    public static int findBestMove(Board board, int depth) {
        // Initialize search state
        clearStatistics();
        startTime = System.currentTimeMillis();

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

        // Find the best move using alpha-beta search
        int bestMove = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        float alpha = Float.NEGATIVE_INFINITY;
        float beta = Float.POSITIVE_INFINITY;

        for (int i = 0; i < rootMoves.size(); i++) {
            int move = rootMoves.get(i);

            // Execute the move
            board.executeMove(move);

            // Search the resulting position
            float score = -alphaBeta(board, depth - 1, -beta, -alpha);

            // Undo the move
            board.undoLastMove();

            logger.info("Move: {} Score: {}", move, score);

            // Update best move if this move is better
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;

                // Update alpha for pruning
                if (score > alpha) {
                    alpha = score;
                }
            }
        }

        // Log search statistics
        long timeElapsed = System.currentTimeMillis() - startTime;
        logger.info("Search completed in {} ms", timeElapsed);
        logger.info("Nodes searched: {}", nodesSearched);
        logger.info("Evaluations: {}", evaluations);
        logger.info("Best move: {} (score: {})", bestMove, bestScore);

        return bestMove;
    }

    /**
     * Basic alpha-beta search algorithm
     */
    private static float alphaBeta(Board board, int depth, float alpha, float beta) {
        // Update node counter
        nodesSearched++;

        // Recursion base case: leaf node
        if (depth == 0) {
            return evaluate(board);
        }

        // Generate legal moves
        MoveList moves = board.getAlliancesLegalMoves(board.getMoveMaker());

        // Detect checkmate/stalemate
        if (moves.size() == 0) {
            if (board.isAllianceInCheck(board.getMoveMaker())) {
                return -1000.0f; // Checkmate (arbitrary large negative value)
            } else {
                return 0.0f; // Stalemate
            }
        }

        // Loop through all legal moves
        for (int i = 0; i < moves.size(); i++) {
            int move = moves.get(i);

            // Execute the move
            board.executeMove(move);

            // Recursively search the resulting position
            float score = -alphaBeta(board, depth - 1, -beta, -alpha);

            // Undo the move
            board.undoLastMove();

            // Alpha-beta pruning
            if (score >= beta) {
                return beta; // Beta cutoff (lower bound)
            }

            // Update alpha
            if (score > alpha) {
                alpha = score;
            }
        }

        // Return the best score (upper bound of the position's value)
        return alpha;
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
     * Clear search statistics
     */
    private static void clearStatistics() {
        nodesSearched = 0;
        evaluations = 0;
    }

    /**
     * Example usage with a fixed depth
     */
    public static void main(String[] args) {
        // Create a board from FEN
        Board board = FenService.createGameFromFEN("r3k2r/ppp2ppp/2n2q2/3Np3/2B5/8/PPP2PPP/R1BQ2K1 w kq - 0 1");

        // Search for best move with depth 4
        int bestMove = LazySMPSearch.findBestMove(board, 2);

        System.out.println("Best move: " + bestMove);
    }
}