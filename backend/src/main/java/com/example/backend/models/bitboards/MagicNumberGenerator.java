package com.example.backend.models.bitboards;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;


/**
 * A class for generating magic numbers for bitboard-based sliding piece move generation.
 * Magic numbers are used to create compact and efficient lookup tables for rook and bishop attacks.
 */
public class MagicNumberGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MagicNumberGenerator.class);
    private static final Random random = new Random();

    public static void main(String[] args) {
        initMagicNumbers();
    }

    /**
     * Initializes and computes magic numbers for rooks and bishops across all board squares.
     * This method runs in parallel to optimize performance.
     */
    private static void initMagicNumbers() {
        final long[] newRookMagicNumbers = new long[64];
        final long[] newBishopMagicNumbers = new long[64];

        IntStream.range(0, ChessUtils.TILES_NUMBER).parallel().forEach(square -> {
            newRookMagicNumbers[square] = findMagicNumber(square, BitBoardUtils.getRookRelevantBits(square), true);
            newBishopMagicNumbers[square] = findMagicNumber(square, BitBoardUtils.getBishopRelevantBits(square), false);
        });

        logger.info("Rook magic numbers: {}", Arrays.toString(newRookMagicNumbers));
        logger.info("Bishop magic numbers: {}", Arrays.toString(newBishopMagicNumbers));
    }

    /**
     * Finds a magic number for a specific square and piece type.
     *
     * @param tileIndex    The index of the tile (0-63).
     * @param relevantBits The number of bits relevant for this square's attack pattern.
     * @param isRook       True if finding a rook's magic number, false for a bishop.
     * @return A suitable magic number for the given parameters.
     */
    private static long findMagicNumber(int tileIndex, int relevantBits, boolean isRook) {
        // holds all possible combinations of occupied tiles that can influence the attack pattern of the given tileIndex
        // 4096 = 2^12: 12 is the largest relevant bit count possible
        final long[] occupancies = new long[4096];

        // holds all attackMasks and blockers are accounted for; each occupancy has an attackMask
        final long[] attackMasks = new long[4096];

        final long[] usedAttacks = new long[4096];

        // get the attackMask for the current piece type (these are the relevant tiles)
        final long attackMask = isRook
                ? BitBoardUtils.getRookAttackMask(tileIndex)
                : BitBoardUtils.getBishopAttackMask(tileIndex);

        // the number of possible ways to occupy the relevant tiles: (2^relevantBits)
        final int numberOfOccupancies = 1 << relevantBits;

        // loop over every possible occupancy
        for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
            // generate all possible combinations of occupied tiles that can influence the attack pattern of the given tileIndex
            occupancies[occupancyIndex] = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);

            // generate the attack masks for every occupancy, taking into account blockers
            attackMasks[occupancyIndex] = isRook
                    ? BitBoardUtils.actualRookAttackMask(tileIndex, occupancies[occupancyIndex]) :
                    BitBoardUtils.actualBishopAttackMask(tileIndex, occupancies[occupancyIndex]);
        }

        // test multiple magic numbers
        for (int iteration = 0; iteration < 1_000_000_000; iteration++) {
            final long magicNumberCandidate = generateMagicNumberCandidate();

            // skip the magic number candidate if the number of set bits (1s) in the top 8 bits (bits 56-63)
            // of the result from multiplying the attack mask and the magic number is less than 6.
            // This ensures that the candidate magic number has sufficient bit spread in the masked portion,
            // which helps in generating a good lookup table for attack patterns.
            if (Long.bitCount((attackMask * magicNumberCandidate) & 0xFF00000000000000L) < 6) {
                continue;
            }

            Arrays.fill(usedAttacks, 0L);
            boolean fail = false;

            // test current magic number
            for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
                // calculate the index of the magic mask.
                // Every occupancy gets multiplied by the magic number. Only the relevant bits are kept,
                // this helps in keeping the mapping of the attack sets as small as possible while remaining unique
                final int magicMaskIndex = (int) ((occupancies[occupancyIndex] * magicNumberCandidate) >>> (64 - relevantBits));

                // check if the magic index works (is unique)
                if (usedAttacks[magicMaskIndex] == 0L) {
                    usedAttacks[magicMaskIndex] = attackMasks[occupancyIndex];
                } else if (usedAttacks[magicMaskIndex] != attackMasks[occupancyIndex]) {
                    // If there is a conflict, the magic number fails
                    fail = true;
                    break;
                }
            }

            // If a valid magic number is found, return it
            if (!fail) {
                return magicNumberCandidate;
            }
        }

        logger.info("Failed to find magic number for: tileIndex={}, relevantBits={}, isRook={}", tileIndex, relevantBits, isRook);
        return 0;
    }

    /**
     * Generates a candidate for a magic number by performing a bitwise AND operation
     * on three randomly generated long values, this increases the number of zero bits.
     * This method is used in the process of finding a suitable magic number for
     * bitboard-based move generation.
     *
     * @return A candidate magic number.
     */
    private static long generateMagicNumberCandidate() {
        return random.nextLong() & random.nextLong() & random.nextLong();
    }
}