package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

public class ModelService {

    // Load the model only once
    private static final SavedModelBundle MODEL = SavedModelBundle.load("src/main/resources/model", "serve");
    private static final Session SESSION = MODEL.session();

    private static final int[][][] REUSED_BOARD = new int[8][8][13];
    private static final float[][][][] REUSED_INPUT_DATA = new float[1][8][8][13];

    private ModelService() {
        throw new ChessException("Can not instantiate this class", ChessExceptionCodes.ILLEGAL_STATE);
    }

    private static void encodeFenStringInPlace(Board board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 13; k++) {
                    REUSED_BOARD[i][j][k] = 0;
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceType pieceType = board.getPieceTypeAtPosition(i * 8 + j);
                Alliance alliance = board.getPieceAllianceAtPosition(i * 8 + j);
                int[] encoding = oneHotEncodePiece(pieceType, alliance);
                System.arraycopy(encoding, 0, REUSED_BOARD[i][j], 0, 13);
            }
        }
    }

    private static void fillBatchArrayFromBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 13; k++) {
                    REUSED_INPUT_DATA[0][i][j][k] = REUSED_BOARD[i][j][k];
                }
            }
        }
    }

    public static float makePrediction(Board board) {
        // 1) Encode the FEN string in place (into REUSED_BOARD)
        encodeFenStringInPlace(board);

        // 2) Copy REUSED_BOARD into REUSED_INPUT_DATA
        fillBatchArrayFromBoard();

        // 3) Create the input tensor from REUSED_INPUT_DATA
        TFloat32 inputTensor = createInputTensor();

        // 4) Encode the extra input features
        float[] extraFeatures = encodeExtraInput(board);
        TFloat32 extraInputTensor = createExtraInputTensor(extraFeatures);

        // 5) Run the model (using the pre-loaded session)
        Tensor outputTensor = SESSION.runner()
                .feed("serving_default_board_input", inputTensor)
                .feed("serving_default_extra_input", extraInputTensor)
                .fetch("StatefulPartitionedCall")
                .run().get(0);

        // 6) Extract the result
        float prediction;
        try (TFloat32 result = (TFloat32) outputTensor) {
            FloatNdArray outputNdArray = NdArrays.ofFloats(result.shape());
            result.copyTo(outputNdArray);
            prediction = extractScalar(outputNdArray);
        }

        // 7) Clean up
        inputTensor.close();
        extraInputTensor.close();

        return prediction;
    }

    /**
     * Creates a TFloat32 tensor from the given 4D float array (shape [1][8][8][13]).
     */
    private static TFloat32 createInputTensor() {
        int batch = ModelService.REUSED_INPUT_DATA.length;        // should be 1
        int rows = ModelService.REUSED_INPUT_DATA[0].length;      // 8
        int columns = ModelService.REUSED_INPUT_DATA[0][0].length; // 8
        int channels = ModelService.REUSED_INPUT_DATA[0][0][0].length; // 13

        int totalElements = batch * rows * columns * channels;
        float[] flatData = new float[totalElements];
        int index = 0;
        for (float[][][] datum : ModelService.REUSED_INPUT_DATA) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    for (int ch = 0; ch < channels; ch++) {
                        flatData[index++] = datum[r][c][ch];
                    }
                }
            }
        }

        Shape shape = Shape.of(batch, rows, columns, channels);
        return TFloat32.tensorOf(shape, DataBuffers.of(flatData));
    }

    /**
     * Creates a TFloat32 tensor from a 1D float array (shape [1, featureLength]).
     */
    private static TFloat32 createExtraInputTensor(float[] extraFeatures) {
        int batch = 1;
        int featureLength = extraFeatures.length;
        float[] flatExtraFeatures = new float[batch * featureLength];
        System.arraycopy(extraFeatures, 0, flatExtraFeatures, 0, featureLength);

        Shape extraShape = Shape.of(batch, featureLength);
        return TFloat32.tensorOf(extraShape, DataBuffers.of(flatExtraFeatures));
    }

    private static int[] oneHotEncodePiece(PieceType pieceType, Alliance alliance) {
        int index = switch (pieceType) {
            case ROOK -> 0;
            case KNIGHT -> 1;
            case BISHOP -> 2;
            case QUEEN -> 3;
            case KING -> 4;
            case PAWN -> 5;
            case null -> 12;
        };
        if (alliance == Alliance.BLACK) {
            index += 6;
        }
        int[] encoding = new int[13];
        encoding[index] = 1;
        return encoding;
    }

    private static float[] encodeExtraInput(Board board) {
        float moveMaker = board.getMoveMaker().isWhite() ? 1f : 0f;
        float whiteKingSide = board.isWhiteKingSideCastleCapable() ? 1f : 0f;
        float whiteQueenSide = board.isWhiteQueenSideCastleCapable() ? 1f : 0f;
        float blackKingSide = board.isBlackKingSideCastleCapable() ? 1f : 0f;
        float blackQueenSide = board.isBlackQueenSideCastleCapable() ? 1f : 0f;
        float enPassantFlag = board.getEnPassantPawnPosition() != -1 ? 1f : 0f;

        return new float[]{moveMaker, whiteKingSide, whiteQueenSide, blackKingSide, blackQueenSide, enPassantFlag};
    }

    /**
     * Extracts a scalar float value from a FloatNdArray with shape [1,1].
     */
    private static float extractScalar(FloatNdArray ndArray) {
        if (ndArray.shape().numDimensions() == 2 &&
                ndArray.shape().size(0) == 1 &&
                ndArray.shape().size(1) == 1) {
            return ndArray.getFloat(0, 0);
        }
        throw new IllegalArgumentException("Output tensor has unexpected shape: " + ndArray.shape());
    }

}
