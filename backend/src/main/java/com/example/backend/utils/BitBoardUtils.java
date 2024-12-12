package com.example.backend.utils;

public class BitBoardUtils {
    /**
     * Constructs an occupancy bitboard for a given index by setting bits at positions
     * specified by the relevant bits in the attack mask.
     *
     * @param occupancyIndex    The index representing a specific combination of occupied squares.
     * @param bitsInMask        The number of relevant bits in the attack mask that are considered for occupation.
     * @param attackMask        The bitboard representing the attack pattern of the piece on the given tile.
     * @return                  A bitboard with bits set at positions specified by the index within the attack mask.
     */
    public static long setOccupancy(int occupancyIndex, int bitsInMask, long attackMask) {
        long occupancy = 0L;

        for (int i = 0; i < bitsInMask; i++) {
            // get the index of the least significant set bit in the attack mask
            int tile = getLs1bIndex(attackMask);
            // remove the least significant set bit from the attack mask
            attackMask = popBit(attackMask, tile);
            // check if the current bit in 'index' is set
            if ((occupancyIndex & (1 << i)) != 0) {
                // set the corresponding bit in the occupancy
                occupancy |= (1L << tile);
            }
        }

        return occupancy;
    }

    public static int getLs1bIndex(long bitboard) {
        if (bitboard != 0) {
            // isolate the least significant set bit
            long isolatedBit = bitboard & -bitboard;
            // count trailing zeros before the LS1B
            return Long.bitCount(isolatedBit - 1);
        } else {
            // return an illegal index if the bitboard is 0
            return -1;
        }
    }

    public static long popBit(long bitboard, int tile) {
        if ((bitboard & (1L << tile)) != 0) {
            // if the bit is set, toggle it using XOR
            return bitboard ^ (1L << tile);
        }
        // return the original bitboard if the bit is not set
        return bitboard;
    }
    /**
     * Counts the number of set bits (1s) in a 64-bit long integer (bitboard).
     *
     * @param bitboard the 64-bit long integer to count the set bits in.
     * @return the number of set bits in the bitboard.
     */
    public static int countBits(long bitboard) {
        int count = 0;

        while (bitboard != 0) {
            count++;
            // clear the least significant 1 bit
            bitboard &= (bitboard - 1);
        }

        return count;
    }
}
