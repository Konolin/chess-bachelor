import numpy as np
from tensorflow.keras.models import load_model

from utils import training_utils

if __name__ == "__main__":
    # 1. Load the model (replace '../model/v4/v4_model.keras' with the actual file path)
    model = load_model('../model/v4/v4_model.keras')

    fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    input_data = training_utils.encode_fen_string(fen)

    # Ensure input_data has shape (8, 8, 13)
    print("Original shape:", input_data.shape)

    # 2. Add a batch dimension so the shape becomes (1, 8, 8, 13)
    input_data = np.expand_dims(input_data, axis=0)
    print("New shape:", input_data.shape)

    # 3. Run prediction
    predictions = model.predict(input_data)

    scaler = training_utils.load_scaler('../model/v4/160k/v4_scaler.pkl')

    # 4. Print the predictions
    # Assuming `predictions` is the output from your model (still scaled)
    original_predictions = scaler.inverse_transform(predictions)
    print("Original Scale Predictions:", original_predictions)
