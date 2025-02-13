import pickle

import numpy as np
from tensorflow.keras.models import load_model

from utils.training_utils import encode_fen_string, load_scaler

if __name__ == "__main__":

    with open("model/v4/v4_scaler.pkl", "rb") as f:
        obj = pickle.load(f)
    print(f"Loaded object type: {type(obj)}")

    pass

    model = load_model("model/v4/v4_model.keras")
    scaler = load_scaler("model/v4/v4_scaler.pkl")

    if scaler is None:
        raise ValueError("Scaler could not be loaded. Ensure scaler.pkl exists and is correctly saved.")


    fen_string = "4k3/p7/1q1p4/2p1B1Q1/2P2P2/1Q3N2/1P1PP1PP/RNB1K2R b KQkq - 0 1"
    x_input = np.array([encode_fen_string(fen_string)])

    raw_prediction = model.predict(x_input)

    black_score = scaler.inverse_transform(raw_prediction.reshape(-1, 1))

    print(f"Predicted Black Score: {black_score[0][0]}")