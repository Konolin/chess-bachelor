package com.example.backend.models.moves;

public class MoveList {
    private int[] moves;
    private int[] scores;
    private int size;

    public MoveList() {
        this.moves = new int[32];
        this.scores = new int[32];
        this.size = 0;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void add(final int move) {
        if (this.size >= this.moves.length) {
            expandCapacity();
        }
        this.moves[this.size] = move;
        this.scores[this.size] = 0;
        this.size++;
    }

    public void addAll(final MoveList moveList) {
        for (int i = 0; i < moveList.size(); i++) {
            add(moveList.get(i));
        }
    }

    private void expandCapacity() {
        final int newCapacity = this.moves.length * 2;
        final int[] newMoves = new int[newCapacity];
        final int[] newScores = new int[newCapacity];

        System.arraycopy(this.moves, 0, newMoves, 0, this.size);
        System.arraycopy(this.scores, 0, newScores, 0, this.size);

        this.moves = newMoves;
        this.scores = newScores;
    }

    public int get(final int index) {
        return this.moves[index];
    }

    public void set(final int index, final int move) {
        this.moves[index] = move;
    }

    /**
     * Set the score for a move at the specified index
     *
     * @param index The index of the move
     * @param score The score to assign
     */
    public void setScore(final int index, final int score) {
        this.scores[index] = score;
    }

    /**
     * Get the score for a move at the specified index
     *
     * @param index The index of the move
     * @return The score of the move
     */
    public int getScore(final int index) {
        return this.scores[index];
    }

    /**
     * Sort the move list by scores in descending order (highest score first)
     * Uses insertion sort which is efficient for small lists
     */
    public void sort() {
        for (int i = 1; i < this.size; i++) {
            final int tempScore = this.scores[i];
            final int tempMove = this.moves[i];
            int j = i - 1;

            // Move elements that are greater than tempScore
            // to one position ahead of their current position
            while (j >= 0 && this.scores[j] < tempScore) {
                this.scores[j + 1] = this.scores[j];
                this.moves[j + 1] = this.moves[j];
                j--;
            }

            this.scores[j + 1] = tempScore;
            this.moves[j + 1] = tempMove;
        }
    }

    public void prioritizePvMove(final int pvMove) {
        // find PV move and move it to the front
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i) == pvMove) {
                // swap with the first move
                if (i > 0) {
                    final int temp = this.get(0);
                    this.set(0, pvMove);
                    this.set(i, temp);

                    final int tempScore = this.getScore(0);
                    this.setScore(0, this.getScore(i));
                    this.setScore(i, tempScore);
                }
                break;
            }
        }
    }

    public int size() {
        return this.size;
    }
}