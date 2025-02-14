package com.example.backend.services;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

public class ModelPredictor {

    /**
     * Creates a TFloat32 tensor from a 4D float array.
     *
     * @param inputData A 4D float array with shape [batch, rows, columns, channels].
     * @return A TFloat32 tensor containing the data.
     */
    public static TFloat32 createInputTensor(float[][][][] inputData) {
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
        try (SavedModelBundle model = SavedModelBundle.load("src/main/resources/model_v4", "serve")) {

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

            // Create input tensor.
            TFloat32 inputTensor = createInputTensor(inputData);

            // Run the model.
            // Adjust feed/fetch names as determined by your SavedModel signature.
            Tensor outputTensor = model.session().runner()
                    .feed("serve_input_layer", inputTensor)
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
     * Dummy FEN string encoder.
     * Replace with your actual implementation that converts a FEN string
     * into a 3D int array of shape [8, 8, channels].
     *
     * @param fenString The FEN string.
     * @return A dummy 3D int array representing the board.
     */
    private static int[][][] encodeFenString(String fenString) {
        int rows = 8;
        int columns = 8;
        int channels = 13;
        int[][][] board = new int[rows][columns][channels];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                board[i][j][0] = 1; // Dummy one-hot encoding for demonstration.
            }
        }
        return board;
    }

    public static void main(String[] args) {
        String fenString = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        float prediction = makePrediction(fenString);
        System.out.println("Model prediction: " + prediction);
    }
}
