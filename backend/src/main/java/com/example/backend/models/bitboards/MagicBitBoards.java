package com.example.backend.models.bitboards;

import com.example.backend.utils.BitBoardUtils;
import com.example.backend.utils.ChessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class MagicBitBoards {
    public static final long[] rookMagicNumbers = {
            324259724000241684L, 1170938170995376706L, 4647723680334356480L, 1224996690898460928L,
            144124005677277200L, 5260213444346389504L, 2449967010570960897L, 144119724702466577L,
            2392538376339488L, -8070098687184855038L, 1157565979162576003L, -9220978949528483840L,
            578008881880410113L, -9184950152173977088L, 72198881315651840L, 11962688659276032L,
            9044032902676480L, 4504424265293824L, 1412873517531153L, 2305984296726300672L,
            11260098663942160L, 5067101484221440L, -9214078964559511024L, 72077385390096741L,
            70370893791236L, -9065604191419351040L, 36100269571833874L, 576478346637475968L,
            578712590772928516L, -9132737083616264064L, 18296015220244548L, 18317872309428355L,
            4614008189610885152L, 81065343593758732L, 143074093174784L, -9222941021812289536L,
            4615117595375306752L, 9009400433869824L, 36618210480500738L, 10052404512836L,
            -8034421597785800704L, 18015499103305760L, 99083598439649280L, 4900479483045871633L,
            7226025619329400896L, 281767039270914L, 5190680208731603012L, 163255763584811009L,
            71476847870464L, 4620697789947445376L, 1201766613909760L, 4629709488209797632L,
            4507997808132224L, 144678173533037056L, -9223090484568370944L, 844433545970944L,
            3459468480536912386L, -5764466646897786622L, -4606546763606126335L, 4648207474535434290L,
            281483835605125L, 1407409310401025L, 1134698183077892L, 6922033728929678338L
    };
    public static final long[] bishopMagicNumbers = {
            577621839820488833L, 577604246577382154L, 5045249859259465728L, 2595203711504515072L,
            4612251236148707392L, 5959490998408450L, 4653167875719168L, 9572356829874176L,
            18172746169323585L, 2882306046775296528L, 2059403062689984L, 144119604250149377L,
            71538050072640L, 290491143925276680L, 4900941225357611008L, -9223353892755992576L,
            18336074416267529L, 1189513286073712808L, 4786105400460032L, -9054486981534269340L,
            10273991279247360L, 579557260784337920L, 396609377022332928L, 564050692145408L,
            4650252574317891584L, 1130307080683682L, 1741213002612867170L, 4684873912570413568L,
            1162008422816686080L, 301266739675392L, 632900934754517504L, 76634346672396292L,
            76851602715582464L, 4649986553192925184L, 19140453037769296L, 563500246237696L,
            576531671348806144L, 158331821949062L, 4794016726058240L, 1165493336023168L,
            91837834530406657L, 72344026767613956L, 4686067998316826628L, -9223371761704238592L,
            1157496710281299456L, -9221014681709706112L, 602566874368528L, 650772346793165312L,
            144264899098329088L, 74381964043878432L, -9218584744939716599L, 689409624072L,
            -9222101334238822080L, 9011618910437504L, 1155191725569081860L, -9204227322876518140L,
            5809828166115904L, 1407662723473416L, 144116360610353156L, 306825317073420812L,
            3247095537765319176L, 437212286227579016L, 4521263119204416L, 199584518938592002L
    };

    private static final Logger logger = LoggerFactory.getLogger(MagicBitBoards.class);
    private static final Random random = new Random();

    public static void main(String[] args) {
        initMagicNumbers();
    }

    private static void initMagicNumbers() {
        long[] newRookMagicNumbers = new long[64];
        long[] newBishopMagicNumbers = new long[64];

        IntStream.range(0, ChessUtils.TILES_NUMBER).parallel().forEach(square -> {
            newRookMagicNumbers[square] = findMagicNumber(square, BitBoardUtils.rookRelevantBits[square], true);
            newBishopMagicNumbers[square] = findMagicNumber(square, BitBoardUtils.bishopRelevantBits[square], false);
        });

        logger.info("Rook magic numbers: {}", Arrays.toString(newRookMagicNumbers));
        logger.info("Bishop magic numbers: {}", Arrays.toString(newBishopMagicNumbers));
    }

    private static long findMagicNumber(int tileIndex, int relevantBits, boolean isRook) {
        // holds all possible combinations of occupied tiles that can influence the attack pattern of the given tileIndex
        // 4096 = 2^12: 12 is the largest relevant bit count possible
        long[] occupancies = new long[4096];

        // holds all attackMasks and blockers are accounted for; each occupancy has an attackMask
        long[] attackMasks = new long[4096];

        long[] usedAttacks = new long[4096];

        // get the attackMask for the current piece type (these are the relevant tiles)
        long attackMask = isRook ? BitBoards.rookAttackMask[tileIndex] : BitBoards.bishopAttackMask[tileIndex];

        // the number of possible ways to occupy the relevant tiles: (2^relevantBits)
        int numberOfOccupancies = 1 << relevantBits;

        // loop over every possible occupancy
        for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
            // generate all possible combinations of occupied tiles that can influence the attack pattern of the given tileIndex
            occupancies[occupancyIndex] = BitBoardUtils.setOccupancy(occupancyIndex, relevantBits, attackMask);

            // generate the attack masks for every occupancy, taking into account blockers
            attackMasks[occupancyIndex] = isRook
                    ? actualRookAttackMask(tileIndex, occupancies[occupancyIndex]) :
                    actualBishopAttackMask(tileIndex, occupancies[occupancyIndex]);
        }

        // test multiple magic numbers
        for (int iteration = 0; iteration < 1_000_000_000; iteration++) {
            long magicNumberCandidate = generateMagicNumberCandidate();

            // skip the magic number candidate if the number of set bits (1s) in the top 8 bits (bits 56-63)
            // of the result from multiplying the attack mask and the magic number is less than 6.
            // This ensures that the candidate magic number has sufficient bit spread in the masked portion,
            // which helps in generating a good lookup table for attack patterns.
            if (BitBoardUtils.countBits((attackMask * magicNumberCandidate) & 0xFF00000000000000L) < 6) {
                continue;
            }

            Arrays.fill(usedAttacks, 0L);
            boolean fail = false;

            // test current magic number
            for (int occupancyIndex = 0; occupancyIndex < numberOfOccupancies; occupancyIndex++) {
                // calculate the index of the magic mask.
                // Every occupancy gets multiplied by the magic number. Only the relevant bits are kept,
                // this helps in keeping the mapping of the attack sets as small as possible while remaining unique
                int magicMaskIndex = (int) ((occupancies[occupancyIndex] * magicNumberCandidate) >>> (64 - relevantBits));

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
     * Generates the bishop attack pattern for a given square, considering blocked squares.
     *
     * @param square The index of the square on the chessboard (0-63).
     * @param block  A bitboard representing blocked squares on the chessboard.
     * @return A bitboard showing the attack pattern for the bishop.
     */
    private static long actualBishopAttackMask(int square, long block) {
        long attacks = 0L;

        // calculate the row and column of the target square
        int targetRow = square / 8;
        int targetCol = square % 8;

        // generate attacks in the top-right direction
        for (int r = targetRow + 1, c = targetCol + 1; r <= 7 && c <= 7; r++, c++) {
            attacks |= (1L << (r * 8 + c));
            if (((1L << (r * 8 + c)) & block) != 0) break;
        }

        // generate attacks in the bottom-right direction
        for (int r = targetRow - 1, c = targetCol + 1; r >= 0 && c <= 7; r--, c++) {
            attacks |= (1L << (r * 8 + c));
            if (((1L << (r * 8 + c)) & block) != 0) break;
        }

        // generate attacks in the top-left direction
        for (int r = targetRow + 1, c = targetCol - 1; r <= 7 && c >= 0; r++, c--) {
            attacks |= (1L << (r * 8 + c));
            if (((1L << (r * 8 + c)) & block) != 0) break;
        }

        // generate attacks in the bottom-left direction
        for (int r = targetRow - 1, c = targetCol - 1; r >= 0 && c >= 0; r--, c--) {
            attacks |= (1L << (r * 8 + c));
            if (((1L << (r * 8 + c)) & block) != 0) break;
        }

        return attacks;
    }

    /**
     * Generates the rook attack pattern for a given square, considering blocked squares.
     *
     * @param square The index of the square on the chessboard (0-63).
     * @param block  A bitboard representing blocked squares on the chessboard.
     * @return A bitboard showing the attack pattern for the rook.
     */
    private static long actualRookAttackMask(int square, long block) {
        long attacks = 0L;

        // calculate the row and column of the target square
        int targetRow = square / 8;
        int targetCol = square % 8;

        // generate attacks in the upward direction (same column)
        for (int r = targetRow + 1; r <= 7; r++) {
            attacks |= (1L << (r * 8 + targetCol));
            if (((1L << (r * 8 + targetCol)) & block) != 0) break;
        }

        // generate attacks in the downward direction (same column)
        for (int r = targetRow - 1; r >= 0; r--) {
            attacks |= (1L << (r * 8 + targetCol));
            if (((1L << (r * 8 + targetCol)) & block) != 0) break;
        }

        // generate attacks in the right direction (same row)
        for (int c = targetCol + 1; c <= 7; c++) {
            attacks |= (1L << (targetRow * 8 + c));
            if (((1L << (targetRow * 8 + c)) & block) != 0) break;
        }

        // generate attacks in the left direction (same row)
        for (int c = targetCol - 1; c >= 0; c--) {
            attacks |= (1L << (targetRow * 8 + c));
            if (((1L << (targetRow * 8 + c)) & block) != 0) break;
        }

        return attacks;
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