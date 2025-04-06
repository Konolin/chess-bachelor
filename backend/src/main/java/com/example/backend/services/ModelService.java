package com.example.backend.services;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.example.backend.exceptions.ChessException;
import com.example.backend.exceptions.ChessExceptionCodes;
import com.example.backend.models.bitboards.PiecesBitBoards;
import com.example.backend.models.board.Board;
import com.example.backend.models.pieces.Alliance;
import com.example.backend.models.pieces.PieceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ModelService {
    private static final OrtEnvironment ENV;
    private static final OrtSession SESSION;
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    static {
        try {
            ENV = OrtEnvironment.getEnvironment();

            // Specify the full path to your model file
            Path modelPath = Paths.get("src/main/resources/resnet_attention_model.onnx");
            SESSION = ENV.createSession(modelPath.toString(), new OrtSession.SessionOptions());

            // Log model input and output names for debugging
            logger.info("Model loaded successfully");
            SESSION.getInputNames().forEach(name ->
                    logger.info("Input name: {}", name));
            SESSION.getOutputNames().forEach(name ->
                    logger.info("Output name: {}", name));
        } catch (OrtException e) {
            logger.error("Failed to load model: {}", e.getMessage(), e);
            throw new ChessException("Failed to load model", ChessExceptionCodes.FAILED_TO_LOAD_MODEL);
        }
    }

    private ModelService() {
        throw new ChessException("Cannot instantiate this class", ChessExceptionCodes.ILLEGAL_STATE);
    }

    public static float makePrediction(Board board) {
        float[][][][] inputTensorData = encodeBoardToTensor(board.getPiecesBBs());
        float[] extraFeatures = encodeExtraInput(board);

        try {
            // Create input tensors with correct shapes and names
            FloatBuffer boardInputBuffer = FloatBuffer.wrap(flatten(inputTensorData));
            FloatBuffer extraInputBuffer = FloatBuffer.wrap(extraFeatures);

            // Create ONNX tensors with correct shapes
            OnnxTensor boardInputTensor = OnnxTensor.createTensor(ENV, boardInputBuffer, new long[]{1, 8, 8, 12});
            OnnxTensor extraInputTensor = OnnxTensor.createTensor(ENV, extraInputBuffer, new long[]{1, extraFeatures.length});

            // Create input map with correct input names from model
            Map<String, OnnxTensor> inputMap = new HashMap<>();
            inputMap.put("board", boardInputTensor);          // Match name from Python model
            inputMap.put("extra_features", extraInputTensor); // Match name from Python model

            // Run inference
            OrtSession.Result results = SESSION.run(inputMap);

            // Extract the result (output is tensor with shape [1, 1])
            // Get the first output tensor
            OnnxTensor output = (OnnxTensor) results.get(0);
            float[][] outputArray = (float[][]) output.getValue();
            float result = outputArray[0][0];

            // Close resources
            boardInputTensor.close();
            extraInputTensor.close();
            output.close();

            // Neural network output is in range [-1, 1], scale to conventional centipawn range if needed
            return result;

        } catch (OrtException e) {
            logger.error("Failed to make prediction: {}", e.getMessage(), e);
            throw new ChessException("Failed to make prediction", ChessExceptionCodes.FAILED_INFERENCE);
        }
    }

    private static float[][][][] encodeBoardToTensor(PiecesBitBoards board) {
        float[][][][] inputTensor = new float[1][8][8][12];

        for (int position = 0; position < 64; position++) {
            int row = position / 8;
            int col = position % 8;

            PieceType pieceType = board.getPieceTypeOfTile(position);
            Alliance pieceAlliance = board.getAllianceOfTile(position);

            if (pieceType != null && pieceAlliance != null) {
                int planeIndex = pieceType.ordinal();
                if (pieceAlliance == Alliance.BLACK) planeIndex += 6;
                inputTensor[0][row][col][planeIndex] = 1f;
            }
        }

        return inputTensor;
    }

    private static float[] encodeExtraInput(Board board) {
        // Initialize all 13 extra features (as per Python model)
        float[] features = new float[13];

        // Fill known features
        features[0] = board.getMoveMaker().isWhite() ? 1f : 0f;  // Side to move

        // Castling rights (4 bits)
        features[1] = board.isWhiteKingSideCastleCapable() ? 1f : 0f;
        features[2] = board.isWhiteQueenSideCastleCapable() ? 1f : 0f;
        features[3] = board.isBlackKingSideCastleCapable() ? 1f : 0f;
        features[4] = board.isBlackQueenSideCastleCapable() ? 1f : 0f;

        // En passant (8 bits, one-hot encoding of file)
        int enPassantPos = board.getEnPassantPawnPosition();
        if (enPassantPos != -1) {
            int file = enPassantPos % 8;  // Get the file (0-7)
            features[5 + file] = 1f;      // Set the corresponding bit
        }

        return features;
    }

    private static float[] flatten(float[][][][] data) {
        int batchSize = data.length;
        int height = data[0].length;
        int width = data[0][0].length;
        int channels = data[0][0][0].length;

        float[] flatData = new float[batchSize * height * width * channels];
        int index = 0;

        // Flatten the 4D tensor to 1D array in the correct order
        for (int b = 0; b < batchSize; b++) {
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    for (int c = 0; c < channels; c++) {
                        flatData[index++] = data[b][h][w][c];
                    }
                }
            }
        }

        return flatData;
    }
}