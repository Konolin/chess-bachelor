package com.example.backend.models.bitboards;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;


/**
 * A class for handling magic bitboards for sliding pieces (rooks and bishops).
 * Magic bitboards are an optimization technique for efficiently calculating sliding piece attacks.
 */
public class MagicBitBoards {
    // precomputed magic bitboards for rooks and bishops
    private static final long[][] ROOK_MAGIC_BIT_BOARDS = initializeSlidingPieceAttacks(true);
    private static final long[][] BISHOP_MAGIC_BIT_BOARDS = initializeSlidingPieceAttacks(false);

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Throws an exception if instantiation is attempted.
     */
    private MagicBitBoards() {
        throw new ChessException("Not instantiable", ChessExceptionCodes.ILLEGAL_STATE);
    }

    /**
     * Computes the attack bitboard for a rook on a given square, given the current occupancy bitboard.
     *
     * @param square    The index of the square (0-63).
     * @param occupancy The bitboard representing occupied squares.
     * @return The bitboard of squares attacked by the rook.
     */
    public static long getRookAttacks(final int square, long occupancy) {
        // get the magic index for the current occupancy
        // bitwise AND to only keep the relevant bits
        occupancy &= BitBoardUtils.getRookAttackMask(square);
        // multiply the occupancy by the magic number for the current square
        occupancy *= BitBoardUtils.getRookMagicNumber(square);
        // shift the occupancy to the right by 64 - relevantBits to remove the "trash" bits
        int magicIndex = (int) (occupancy >>> (64 - BitBoardUtils.getRookRelevantBits(square)));
        // return the magic bitboard for the current square and occupancy
        return ROOK_MAGIC_BIT_BOARDS[square][magicIndex];
    }

    /**
     * Computes the attack bitboard for a bishop on a given square, given the current occupancy bitboard.
     *
     * @param square    The index of the square (0-63).
     * @param occupancy The bitboard representing occupied squares.
     * @return The bitboard of squares attacked by the bishop.
     */
    public static long getBishopAttacks(final int square, long occupancy) {
        occupancy &= BitBoardUtils.getBishopAttackMask(square);
        occupancy *= BitBoardUtils.getBishopMagicNumber(square);
        final int magicIndex = (int) (occupancy >>> (64 - BitBoardUtils.getBishopRelevantBits(square)));
        return BISHOP_MAGIC_BIT_BOARDS[square][magicIndex];
    }

    /**
     * Precomputes the magic bitboards for sliding pieces (rooks or bishops).
     *
     * @param isRook True for rooks, false for bishops.
     * @return A 2D array containing precomputed attack bitboards for all squares and occupancies.
     */
    private static long[][] initializeSlidingPieceAttacks(final boolean isRook) {
        final long[][] magicBitBoards = new long[64][isRook ? 4096 : 512];

        for (int square = 0; square < ChessUtils.TILES_NUMBER; square++) {
            // get the attackMask for the current piece type (these are the relevant tiles)
            final long attackMask = isRook
                    ? BitBoardUtils.getRookAttackMask(square)
                    : BitBoardUtils.getBishopAttackMask(square);

            // the number of relevant bits for the current piece type and tile
            final int relevantBits = isRook
                    ? BitBoardUtils.getRookRelevantBits(square)
                    : BitBoardUtils.getBishopRelevantBits(square);

            // the number of possible ways to occupy the relevant tiles: (2^relevantBits)
            final int numberOfOccupancies = 1 << relevantBits;

            // loop over every possible occupancy
            for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
                if (isRook) {
                    // generate the occupancy for the current occupancyIndex
                    final long occupancy = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);

                    // get the magic index for the current occupancy
                    // this index removes the "trash" bits from the occupancy to create a more efficient hash
                    final int magicIndex = (int) ((occupancy * BitBoardUtils.getRookMagicNumber(square)) >>> (64 - BitBoardUtils.getRookRelevantBits(square)));

                    // set the magic bitboard for the current square and occupancy
                    magicBitBoards[square][magicIndex] = BitBoardUtils.actualRookAttackMask(square, occupancy);
                } else {
                    final long occupancy = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);
                    final int magicIndex = (int) ((occupancy * BitBoardUtils.getBishopMagicNumber(square)) >>> (64 - BitBoardUtils.getBishopRelevantBits(square)));
                    magicBitBoards[square][magicIndex] = BitBoardUtils.actualBishopAttackMask(square, occupancy);
                }
            }
        }
        return magicBitBoards;
    }
}
