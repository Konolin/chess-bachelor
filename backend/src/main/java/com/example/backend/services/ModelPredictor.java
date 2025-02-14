package com.example.backend.services;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

import java.util.Objects;

public class ModelPredictor {

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
        for (int b = 0; b < batch; b++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    for (int ch = 0; ch < channels; ch++) {
                        flatData[index++] = inputData[b][r][c][ch];
                    }
                }
            }
        }
        Shape shape = Shape.of(batch, rows, columns, channels);
        return TFloat32.tensorOf(shape, DataBuffers.of(flatData));
    }

    /**
     * Loads the SavedModel, encodes the input FEN string, creates an input tensor,
     * runs the model, and returns the prediction.
     *
     * @param fenString The chess board state in FEN format.
     * @return The model's prediction as a float.
     */
    public static float makePrediction(String fenString) {
        // Adjust the model path and tag ("serve") as needed.
        try (SavedModelBundle model = SavedModelBundle.load("src/main/resources/model", "serve")) {

            // Encode the FEN string into a 3D int array.
            int[][][] encodedBoard = encodeFenString(fenString);
            int rows = encodedBoard.length;
            int columns = encodedBoard[0].length;
            int channels = encodedBoard[0][0].length;

            // Add batch dimension to create a 4D float array: [1, rows, columns, channels].
            float[][][][] inputData = new float[1][rows][columns][channels];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    for (int k = 0; k < channels; k++) {
                        inputData[0][i][j][k] = encodedBoard[i][j][k];
                    }
                }
            }

            float[] extraFeatures = encodeExtraInput(fenString);

            // Prepare extra input data with a batch dimension. In this case, shape is [1, extraFeatures.length].
            int batch = 1;
            int featureLength = extraFeatures.length;
            float[] flatExtraFeatures = new float[batch * featureLength];
            // Since batch is 1, simply copy extraFeatures.
            System.arraycopy(extraFeatures, 0, flatExtraFeatures, 0, featureLength);

            // Create a Shape object for the extra features.
            Shape extraShape = Shape.of(batch, featureLength);

            // Create the extra input tensor.
            TFloat32 extraInputTensor = TFloat32.tensorOf(extraShape, DataBuffers.of(flatExtraFeatures));

            // Create input tensor.
            TFloat32 inputTensor = createInputTensor(inputData);

            // Run the model.
            // Adjust feed/fetch names as determined by your SavedModel signature.
            Tensor outputTensor = model.session().runner()
                    .feed("serve_board_input", inputTensor)
                    .feed("serve_extra_input", extraInputTensor)
                    .fetch("StatefulPartitionedCall")
                    .run().get(0);

            // Extract prediction from output tensor.
            TFloat32 result = (TFloat32) outputTensor;
            FloatNdArray outputNdArray = NdArrays.ofFloats(result.shape());
            result.copyTo(outputNdArray);

            float prediction = extractScalar(outputNdArray);

            // Clean up tensors.
            result.close();
            inputTensor.close();

            return prediction;
        }
    }

    /**
     * Extracts a scalar float value from an NdArray.
     * Adjusts for the output tensor's rank.
     *
     * @param array The NdArray holding the output.
     * @return The extracted scalar value.
     */
    private static float extractScalar(FloatNdArray array) {
        long rank = array.shape().numDimensions();
        if (rank == 0) {
            return array.getFloat(); // Scalar tensor.
        } else if (rank == 1) {
            // Assuming shape [1]
            return array.getFloat(0);
        } else if (rank == 2) {
            // Assuming shape [1, 1]
            return array.getFloat(0, 0);
        } else {
            // For higher ranks, you might need to adjust this logic.
            // Here, we simply return the element at index 0...0.
            long[] indices = new long[(int) rank];
            for (int i = 0; i < rank; i++) {
                indices[i] = 0;
            }
            return array.getFloat(indices);
        }
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
        // Get the board's string representation from your FenService.
        // This string should have rows separated by "\n".
        String boardString = FenService.createGameFromFEN(fenString).toString();
        // Remove spaces from the board string.
        boardString = boardString.replace(" ", "");
        // Split the board string into rows.
        String[] rows = boardString.split("\n");
        int numRows = rows.length;
        // Assume every row has the same number of columns.
        int numCols = rows[0].length();
        // The number of channels is the length of our pieces string ("rnbqkpRNBQKP.").
        int channels = 13;

        // Create the board array.
        int[][][] board = new int[numRows][numCols][channels];
        for (int i = 0; i < numRows; i++) {
            String row = rows[i];
            for (int j = 0; j < numCols; j++) {
                char piece = row.charAt(j);
                // One-hot encode the piece.
                int[] encoding = oneHotEncodePiece(piece);
                board[i][j] = encoding;
            }
        }
        return board;
    }

    /**
     * One-hot encodes a chess piece.
     * The encoding vector is of length 13 corresponding to the pieces: "rnbqkpRNBQKP."
     *
     * @param piece The character representing the piece.
     * @return An int array of length 13 that is all zeros except a 1 at the index of the piece.
     */
    private static int[] oneHotEncodePiece(char piece) {
        String pieces = "rnbqkpRNBQKP.";
        int channels = pieces.length(); // 13 channels
        int[] encoding = new int[channels];
        int index = pieces.indexOf(piece);
        if (index != -1) {
            encoding[index] = 1;
        }
        return encoding;
    }

    private static float[] encodeExtraInput(String fen) {
        final String[] fenPartitions = fen.trim().split("");
        final String moveMakerString = fenPartitions[1];
        final String castleString = fenPartitions[2];
        final String enPassantString = fenPartitions[3];

        float moveMaker = Objects.equals(moveMakerString, "w") ? 1f : 0f;

        float whiteKingSide = castleString.contains("K") ? 1f : 0f;
        float whiteQueenSide = castleString.contains("Q") ? 1f : 0f;
        ;
        float blackKingSide = castleString.contains("k") ? 1f : 0f;
        ;
        float blackQueenSide = castleString.contains("q") ? 1f : 0f;
        ;

        float enPassantFlag = !Objects.equals(enPassantString, "-") ? 1f : 0f;

        return new float[]{moveMaker, whiteKingSide, whiteQueenSide, blackKingSide, blackQueenSide, enPassantFlag};
    }
}
