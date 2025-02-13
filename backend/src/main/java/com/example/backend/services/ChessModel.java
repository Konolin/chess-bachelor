package com.example.backend.services;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.util.ArrayList;
import java.util.List;
public class ChessModel {
    // string representing all possible chess pieces and an empty tile.
    private static final String PIECES = "rnbqkpRNBQKP-";

    public static float evaluatePosition(final String fen) {
        // Try-with-resources will automatically close the model and session
        try (SavedModelBundle model = SavedModelBundle.load("model");
             // Create a session from the model
             Session session = model.session()) {

            // Encode the board represented in fen notation:
            int[][][] encodedFenArray = encodeFenString(fen);

            // The model typically wants a float input, so we convert the int array to float.
            // We also add a batch dimension at the front, e.g. shape [1, 8, 8, 12].
            float[][][][] inputData = new float[1][encodedFenArray.length]
                    [encodedFenArray[0].length]
                    [encodedFenArray[0][0].length];

            for (int i = 0; i < encodedFenArray.length; i++) {
                for (int j = 0; j < encodedFenArray[i].length; j++) {
                    for (int k = 0; k < encodedFenArray[i][j].length; k++) {
                        inputData[0][i][j][k] = (float) encodedFenArray[i][j][k];
                    }
                }
            }

            // Create the Tensor from the float array
            // You must feed it into the correct input operation name from your model.
            // (Replace "serving_default_input_1" with the actual input name.)
            try (Tensor<?> inputTensor = Tensors.create(inputData)) {

                // Run inference:
                // Replace "serving_default_input_1" with your real input op name
                // Replace "StatefulPartitionedCall" (or "serving_default_output_0", etc.)
                // with the actual output op name for your model.
                List<Tensor<?>> outputs = session.runner()
                        .feed("serving_default_input_1", inputTensor)
                        .fetch("StatefulPartitionedCall")
                        .run();

                // Extract the single float result
                try (Tensor<?> resultTensor = outputs.getFirst()) {
                    // If the model returns a single scalar, you can copy it to a float array of shape [1]
                    float[] scalar = new float[1];
                    resultTensor.copyTo(scalar);

                    return scalar[0];  // The predicted evaluation
                }
            }
        }
        // If something goes wrong, handle/log the exception, etc.
        // For brevity, just return 0 here or rethrow.
        catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }



    /**
     * Converts a FEN string into a one-hot encoded representation.
     * The board is converted into an 8x8x12 integer array where each piece type
     * is represented as a one-hot encoded vector.
     *
     * @param fenString A valid FEN string representing a chess position.
     * @return A 3D array (8x8x12) where each tile is represented as a one-hot encoded vector.
     */
    private static int[][][] encodeFenString(final String fenString) {
        // convert FEN string into a board representation and remove spaces
        String boardString = FenService.createGameFromFEN(fenString).toString().replace(" ", "");

        // a list to store encoded board rows
        List<int[][]> boardList = new ArrayList<>();

        // go through each row of the board and encode each tile
        for (String row : boardString.split("\n")) {
            List<int[]> rowList = new ArrayList<>();
            for (char tile : row.toCharArray()) {
                rowList.add(oneHotEncodeTile(tile));
            }
            boardList.add(rowList.toArray(new int[0][]));
        }

        return boardList.toArray(new int[0][][]);
    }

    /**
     * One-hot encodes a chess piece character.
     * The encoding is a 12-element array where the position corresponding to the
     * piece character is set to 1, while all other positions are 0.
     *
     * @param piece A character representing a chess piece ('r', 'n', 'b', 'q', 'k', 'p', 'R', 'N', 'B', 'Q', 'K', 'P')
     *              or an empty square ('-').
     * @return A 12-element integer array representing the one-hot encoding of the piece.
     */
    private static int[] oneHotEncodeTile(final char piece) {
        int[] encoding = new int[PIECES.length()];
        int index = PIECES.indexOf(piece);
        if (index != -1) {
            encoding[index] = 1; // set the corresponding index to 1
        }
        return encoding;
    }
}
