package com.example.backend.models.moves;

public class MoveList {
    private int[] moves;
    private int[] scores;
    private int size;

    public MoveList() {
        moves = new int[32];
        scores = new int[32];
        size = 0;
    }

    public void add(int move) {
        if (size >= moves.length) {
            expandCapacity();
        }
        moves[size] = move;
        scores[size] = 0;
        size++;
    }

    public void addAll(MoveList moveList) {
        for (int i = 0; i < moveList.size(); i++) {
            add(moveList.get(i));
        }
    }

    private void expandCapacity() {
        int newCapacity = moves.length * 2;
        int[] newMoves = new int[newCapacity];
        int[] newScores = new int[newCapacity];

        System.arraycopy(moves, 0, newMoves, 0, size);
        System.arraycopy(scores, 0, newScores, 0, size);

        moves = newMoves;
        scores = newScores;
    }

    public int get(int index) {
        return moves[index];
    }

    public void set(int index, int move) {
        moves[index] = move;
    }

    /**
     * Set the score for a move at the specified index
     * @param index The index of the move
     * @param score The score to assign
     */
    public void setScore(int index, int score) {
        scores[index] = score;
    }

    /**
     * Get the score for a move at the specified index
     * @param index The index of the move
     * @return The score of the move
     */
    public int getScore(int index) {
        return scores[index];
    }

    /**
     * Sort the move list by scores in descending order (highest score first)
     * Uses insertion sort which is efficient for small lists
     */
    public void sort() {
        for (int i = 1; i < size; i++) {
            int tempScore = scores[i];
            int tempMove = moves[i];
            int j = i - 1;

            // Move elements that are greater than tempScore
            // to one position ahead of their current position
            while (j >= 0 && scores[j] < tempScore) {
                scores[j + 1] = scores[j];
                moves[j + 1] = moves[j];
                j--;
            }

            scores[j + 1] = tempScore;
            moves[j + 1] = tempMove;
        }
    }

    public int size() {
        return size;
    }
}