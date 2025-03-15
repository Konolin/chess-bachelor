package com.example.backend.services;

import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.utils.ChessUtils;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

import java.util.Objects;

public class ModelService {
    private static final SavedModelBundle model;

    private ModelService() {
        throw new ChessException("Can not instantiate this class", ChessExceptionCodes.ILLEGAL_STATE);
    }

    static {
        model = SavedModelBundle.load("src/main/resources/model", "serve");
    }

    /**
     * Loads the model, encodes the inputs (fenString and extra features), and makes a prediction.
     *
     * @param fenString The chess board state in FEN format.
     * @return The model's prediction as a float.
     */
    public static float makePrediction(String fenString) {
        // encode the FEN string into a 3D int array.
        int[][][] encodedBoard = encodeFenString(fenString);
        int rows = encodedBoard.length;
        int columns = encodedBoard[0].length;
        int channels = encodedBoard[0][0].length;

        // add a batch dimension to create a 4D float array: [1, rows, columns, channels]
        float[][][][] inputData = new float[1][rows][columns][channels];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                for (int k = 0; k < channels; k++) {
                    inputData[0][i][j][k] = encodedBoard[i][j][k];
                }
            }
        }

        // create the board input tensor
        TFloat32 inputTensor = createInputTensor(inputData);

        // encode the extra input features
        float[] extraFeatures = encodeExtraInput(fenString);

        // prepare extra input data with a batch dimension. Shape: [1, extraFeatures.length]
        int batch = 1;
        int featureLength = extraFeatures.length;
        float[] flatExtraFeatures = new float[batch * featureLength];
        System.arraycopy(extraFeatures, 0, flatExtraFeatures, 0, featureLength);

        // create the extra input tensor
        Shape extraShape = Shape.of(batch, featureLength);
        TFloat32 extraInputTensor = TFloat32.tensorOf(extraShape, DataBuffers.of(flatExtraFeatures));

        // run the model
        Tensor outputTensor = model.session().runner()
                .feed("serving_default_board_input", inputTensor)
                .feed("serving_default_extra_input", extraInputTensor)
                .fetch("StatefulPartitionedCall")
                .run().get(0);

        // extract prediction from the output tensor
        TFloat32 result = (TFloat32) outputTensor;
        FloatNdArray outputNdArray = NdArrays.ofFloats(result.shape());
        result.copyTo(outputNdArray);
        float prediction = extractScalar(outputNdArray);

        // clean up tensors
        result.close();
        inputTensor.close();
        extraInputTensor.close();

        return prediction;
    }

    /**
     * Creates a TFloat32 tensor from a 4D float array.
     *
     * @param inputData A 4D float array with shape [batch, rows, columns, channels].
     * @return A TFloat32 tensor containing the data.
     */
    private static TFloat32 createInputTensor(float[][][][] inputData) {
        int batch = inputData.length;
        int rows = inputData[0].length;
        int columns = inputData[0][0].length;
        int channels = inputData[0][0][0].length;

        int totalElements = batch * rows * columns * channels;
        float[] flatData = new float[totalElements];
        int index = 0;
        for (float[][][] inputDatum : inputData) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    for (int ch = 0; ch < channels; ch++) {
                        flatData[index++] = inputDatum[r][c][ch];
                    }
                }
            }
        }
        Shape shape = Shape.of(batch, rows, columns, channels);
        return TFloat32.tensorOf(shape, DataBuffers.of(flatData));
    }

    /**
     * Converts a FEN string into a 3D int array representing the board.
     * Each square is represented by a one-hot encoded vector of length 13,
     * corresponding to the pieces "rnbqkpRNBQKP.".
     *
     * @param fenString The chess board state in FEN format.
     * @return A 3D int array with shape [rows][columns][channels].
     */
    private static int[][][] encodeFenString(String fenString) {
        // Convert the FEN string to a board string so every tile can be processed individually.
        String boardString = FenService.convertFENToStringBoard(fenString);
        // Remove all spaces from the board string.
        boardString = boardString.replace(" ", "");
        // Split the board string into rows.
        String[] rows = boardString.split("\n");

        // Define the board dimensions (8x8x13).
        int numRows = ChessUtils.TILES_PER_ROW;
        int numCols = ChessUtils.TILES_PER_ROW;
        int channels = 13;

        // Create the board array and encode every tile.
        int[][][] board = new int[numRows][numCols][channels];
        for (int i = 0; i < numRows; i++) {
            String row = rows[i];
            for (int j = 0; j < numCols; j++) {
                char piece = row.charAt(j);
                int[] encoding = oneHotEncodePiece(piece);
                board[i][j] = encoding;
            }
        }
        return board;
    }

    /**
     * One-hot encodes a chess piece.
     * The encoding vector is of length 13 corresponding to the pieces: "rnbqkpRNBQKP.".
     *
     * @param piece The character representing the piece.
     * @return An int array of length 13 that is all zeros except a 1 at the index of the piece.
     */
    private static int[] oneHotEncodePiece(char piece) {
        String pieces = "rnbqkpRNBQKP.";
        int channels = pieces.length();
        int[] encoding = new int[channels];
        int index = pieces.indexOf(piece);
        if (index != -1) {
            encoding[index] = 1;
        }
        return encoding;
    }

    /**
     * Encodes the extra input features from the FEN string.
     * The features include the move maker, castling rights, and en passant flag.
     *
     * @param fen The chess board state in FEN format.
     * @return A float array of the encoded features.
     */
    private static float[] encodeExtraInput(String fen) {
        // Split the FEN string by whitespace to extract its components.
        final String[] fenPartitions = fen.trim().split("\\s+");
        if (fenPartitions.length < 4) {
            throw new IllegalArgumentException("Invalid FEN string: " + fen);
        }
        final String moveMakerString = fenPartitions[1];
        final String castleString = fenPartitions[2];
        final String enPassantString = fenPartitions[3];

        float moveMaker = Objects.equals(moveMakerString, "w") ? 1f : 0f;
        float whiteKingSide = castleString.contains("K") ? 1f : 0f;
        float whiteQueenSide = castleString.contains("Q") ? 1f : 0f;
        float blackKingSide = castleString.contains("k") ? 1f : 0f;
        float blackQueenSide = castleString.contains("q") ? 1f : 0f;
        float enPassantFlag = !Objects.equals(enPassantString, "-") ? 1f : 0f;

        return new float[]{moveMaker, whiteKingSide, whiteQueenSide, blackKingSide, blackQueenSide, enPassantFlag};
    }

    /**
     * Extracts a scalar float value from a FloatNdArray.
     *
     * @param ndArray The FloatNdArray containing the output tensor data.
     * @return The extracted scalar float value.
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
