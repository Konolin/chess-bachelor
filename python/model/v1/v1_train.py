from datetime import datetime

import numpy as np
import utils.training_utils as training_utils
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Flatten


def main():
    train_df, val_df = training_utils.fetch_data()

    x_train = np.stack(train_df['fen'].apply(training_utils.encode_fen_string))
    y_train = train_df['black_score']
    x_val = np.stack(val_df['fen'].apply(training_utils.encode_fen_string))
    y_val = val_df['black_score']

    model = Sequential([
        Flatten(),
        Dense(1024, activation='relu'),
        Dense(64, activation='relu'),
        Dense(1),
    ])

    model.compile(
        optimizer='rmsprop',
        loss='mean_squared_error')

    history = model.fit(
        x_train,
        y_train,
        epochs=50,
        validation_data=(x_val, y_val))

    metadata = {
        "model_name": "model_v1",
        "architecture": "Dense (1024-64-1)",
        "optimizer": "rmsprop",
        "loss": "mean_squared_error",
        "epochs": 50,
        "date": str(datetime.now()),
        "training_data_size": len(train_df),
    }

    training_utils.save(model, history.history, 1, metadata)


if __name__ == "__main__":
    main()
