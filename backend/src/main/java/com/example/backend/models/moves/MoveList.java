package com.example.backend.models.moves;

public class MoveList {
    private int[] moves;
    private int size;

    public MoveList() {
        moves = new int[32];
        size = 0;
    }

    public void add(int move) {
        if (size >= moves.length) {
            expandCapacity();
        }
        moves[size++] = move;
    }

    public void addAll(MoveList moveList) {
        for (int i = 0; i < moveList.size(); i++) {
            add(moveList.get(i));
        }
    }

    private void expandCapacity() {
        int newCapacity = moves.length * 2;
        int[] newArray = new int[newCapacity];
        System.arraycopy(moves, 0, newArray, 0, size);
        moves = newArray;
    }

    public int get(int index) {
        return moves[index];
    }

    public void set(int index, int move) {
        moves[index] = move;
    }

    public void moveToFront(int move) {
        for (int i = 0; i < size; i++) {
            if (moves[i] == move) {
                for (int j = i; j > 0; j--) {
                    moves[j] = moves[j - 1];
                }
                moves[0] = move;
                return;
            }
        }
    }

    public int size() {
        return size;
    }
}
