package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;

public class MagicBitBoards {
    public static final long[][] rookMagicBitBoards = initializeSlidingPieceAttacks(true);
    public static final long[][] bishopMagicBitBoards = initializeSlidingPieceAttacks(false);

    private MagicBitBoards() {
        throw new ChessException("Not instantiable", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static long getRookAttacks(int square, long occupancy) {
        // get the magic index for the current occupancy
        // bitwise AND to only keep the relevant bits
        occupancy &= BitBoardUtils.ROOK_ATTACK_MASK[square];
        // multiply the occupancy by the magic number for the current square
        occupancy *= BitBoardUtils.rookMagicNumbers[square];
        // shift the occupancy to the right by 64 - relevantBits to remove the "trash" bits
        int magicIndex = (int) (occupancy >>> (64 - BitBoardUtils.rookRelevantBits[square]));
        // return the magic bitboard for the current square and occupancy
        return rookMagicBitBoards[square][magicIndex];
    }

    public static long getBishopAttacks(int square, long occupancy) {
        occupancy &= BitBoardUtils.BISHOP_ATTACK_MASK[square];
        occupancy *= BitBoardUtils.bishopMagicNumbers[square];
        int magicIndex = (int) (occupancy >>> (64 - BitBoardUtils.bishopRelevantBits[square]));
        return bishopMagicBitBoards[square][magicIndex];
    }

    private static long[][] initializeSlidingPieceAttacks(boolean isRook) {
        long[][] magicBitBoards = new long[64][isRook ? 4096 : 512];

        for (int square = 0; square < ChessUtils.TILES_NUMBER; square++) {
            // get the attackMask for the current piece type (these are the relevant tiles)
            long attackMask = isRook
                    ? BitBoardUtils.ROOK_ATTACK_MASK[square]
                    : BitBoardUtils.BISHOP_ATTACK_MASK[square];

            // the number of relevant bits for the current piece type and tile
            int relevantBits = isRook
                    ? BitBoardUtils.rookRelevantBits[square]
                    : BitBoardUtils.bishopRelevantBits[square];

            // the number of possible ways to occupy the relevant tiles: (2^relevantBits)
            int numberOfOccupancies = 1 << relevantBits;

            // loop over every possible occupancy
            for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
                if (isRook) {
                    // generate the occupancy for the current occupancyIndex
                    long occupancy = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);

                    // get the magic index for the current occupancy
                    // this index removes the "trash" bits from the occupancy to create a more efficient hash
                    int magicIndex = (int) ((occupancy * BitBoardUtils.rookMagicNumbers[square]) >>> (64 - BitBoardUtils.rookRelevantBits[square]));

                    // set the magic bitboard for the current square and occupancy
                    magicBitBoards[square][magicIndex] = BitBoardUtils.actualRookAttackMask(square, occupancy);
                } else {
                    long occupancy = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);
                    int magicIndex = (int) ((occupancy * BitBoardUtils.bishopMagicNumbers[square]) >>> (64 - BitBoardUtils.bishopRelevantBits[square]));
                    magicBitBoards[square][magicIndex] = BitBoardUtils.actualBishopAttackMask(square, occupancy);
                }
            }
        }

        return magicBitBoards;
    }
}
