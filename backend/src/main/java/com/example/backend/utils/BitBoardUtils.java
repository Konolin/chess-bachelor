package com.example.backend.utils;

public class BitBoardUtils {
    // bishop relevant attack mask bit count for every square on board
    public static final int[] bishopRelevantBits = {
            6, 5, 5, 5, 5, 5, 5, 6,
            5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 7, 7, 7, 7, 5, 5,
            5, 5, 7, 9, 9, 7, 5, 5,
            5, 5, 7, 9, 9, 7, 5, 5,
            5, 5, 7, 7, 7, 7, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5,
            6, 5, 5, 5, 5, 5, 5, 6
    };

    // rook relevant attack mask bit count for every square on board
    public static final int[] rookRelevantBits = {
            12, 11, 11, 11, 11, 11, 11, 12,
            11, 10, 10, 10, 10, 10, 10, 11,
            11, 10, 10, 10, 10, 10, 10, 11,
            11, 10, 10, 10, 10, 10, 10, 11,
            11, 10, 10, 10, 10, 10, 10, 11,
            11, 10, 10, 10, 10, 10, 10, 11,
            11, 10, 10, 10, 10, 10, 10, 11,
            12, 11, 11, 11, 11, 11, 11, 12
    };

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

    public static final long[] rookAttackMask = computeRookAttackMask();
    public static final long[] bishopAttackMask = computeBishopAttackMask();

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
     * Generates the rook attack pattern for a given square, considering blocked squares.
     *
     * @param square The index of the square on the chessboard (0-63).
     * @param block  A bitboard representing blocked squares on the chessboard.
     * @return A bitboard showing the attack pattern for the rook.
     */
    public static long actualRookAttackMask(int square, long block) {
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
     * Generates the bishop attack pattern for a given square, considering blocked squares.
     *
     * @param square The index of the square on the chessboard (0-63).
     * @param block  A bitboard representing blocked squares on the chessboard.
     * @return A bitboard showing the attack pattern for the bishop.
     */
    public static long actualBishopAttackMask(int square, long block) {
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
     * Computes the relevant occupancy masks for rook moves on a chessboard.
     * <p>
     * This method generates a lookup table where each entry represents the relevant
     * occupancy mask for a specific square on the board. The relevant occupancy mask
     * includes all squares in the same row and column as the rook, excluding edge squares
     * and the square where the rook is currently located.
     * <p>
     * For example:
     * - For a rook on `a1`, the mask will include `a2` to `a7` (vertical) and `b1` to `g1` (horizontal).
     * - For a rook on `d4`, the mask will include `d2` to `d7`, `b4` to `c4`, and `e4` to `g4`, excluding `d1` and `d8`.
     *
     * @return a long array of size 64, where each entry corresponds to the relevant
     *         occupancy mask for the rook at a specific square (indexed from 0 for `a1`
     *         to 63 for `h8`).
     */
    private static long[] computeRookAttackMask() {
        long[] table = new long[ChessUtils.TILES_NUMBER];

        // iterate over all squares of the board, creating a mask for every tile
        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            long mask = 0L;
            int row = i / 8;
            int col = i % 8;

            // generate relevant occupancy for the row
            for (int j = col + 1; j < 7; j++) { // exclude edges
                mask |= 1L << (row * 8 + j);
            }
            for (int j = col - 1; j > 0; j--) { // exclude edges
                mask |= 1L << (row * 8 + j);
            }

            // Generate relevant occupancy for the column
            for (int j = row + 1; j < 7; j++) { // exclude edges
                mask |= 1L << (j * 8 + col);
            }
            for (int j = row - 1; j > 0; j--) { // exclude edges
                mask |= 1L << (j * 8 + col);
            }

            table[i] = mask;
        }

        return table;
    }

    /**
     * Computes the relevant occupancy masks for bishop moves on a chessboard.
     * <p>
     * This method generates a lookup table where each entry represents the relevant
     * occupancy mask for a specific square on the board. The relevant occupancy mask
     * includes all squares along the diagonals originating from the bishop's position,
     * excluding edge squares and the square where the bishop is currently located.
     * <p>
     * For example:
     * - For a bishop on `a1`, the mask will include `b2` to `g7` (top-right diagonal).
     * - For a bishop on `d4`, the mask will include squares such as `c3`, `b2`, `e5`, `f6`, etc.,
     *   excluding edge squares and those not part of the diagonals.
     *
     * @return a long array of size 64, where each entry corresponds to the relevant
     *         occupancy mask for the bishop at a specific square (indexed from 0 for `a1`
     *         to 63 for `h8`).
     */
    private static long[] computeBishopAttackMask() {
        long[] table = new long[ChessUtils.TILES_NUMBER];

        for (int i = 0; i < ChessUtils.TILES_NUMBER; i++) {
            long mask = 0L;
            int row = i / 8;
            int col = i % 8;

            // Calculate relevant squares for the top-left to bottom-right diagonal
            for (int r = row + 1, c = col + 1; r < 7 && c < 7; r++, c++) {
                mask |= (1L << (r * 8 + c));
            }
            for (int r = row - 1, c = col - 1; r > 0 && c > 0; r--, c--) {
                mask |= (1L << (r * 8 + c));
            }

            // Calculate relevant squares for the top-right to bottom-left diagonal
            for (int r = row + 1, c = col - 1; r < 7 && c > 0; r++, c--) {
                mask |= (1L << (r * 8 + c));
            }
            for (int r = row - 1, c = col + 1; r > 0 && c < 7; r--, c++) {
                mask |= (1L << (r * 8 + c));
            }

            table[i] = mask;
        }

        return table;
    }

    public static String bitBoardFormatedString(long bitboard) {
        String bitboardString = String.format("%64s", Long.toBinaryString(bitboard)).replace(' ', '0');
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bitboardString.length(); i++) {
            sb.append(bitboardString.charAt(i));
            if ((i + 1) % 8 == 0) {
                sb.append("\n");
            }
        }
        return sb.reverse().toString();
    }
}
