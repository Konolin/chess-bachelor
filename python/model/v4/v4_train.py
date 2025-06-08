from datetime import datetime

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Flatten, Dropout
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.optimizers import Adam
from sklearn.preprocessing import StandardScaler
import numpy as np
import utils.training_utils as training_utils


def main():
    train_df, val_df = training_utils.fetch_data()

    x_train = np.stack(train_df['fen'].apply(training_utils.encode_fen_string))
    x_val = np.stack(val_df['fen'].apply(training_utils.encode_fen_string))

    # Normalize y_train and y_val using StandardScaler
    scaler = StandardScaler()
    y_train = scaler.fit_transform(train_df[['black_score']])
    y_val = scaler.transform(val_df[['black_score']])

    model = Sequential([
        Flatten(),
        Dense(512, activation='relu', kernel_regularizer=l2(0.001)),
        Dropout(0.2),
        Dense(128, activation='relu', kernel_regularizer=l2(0.001)),
        Dropout(0.2),
        Dense(64, activation='relu'),
        Dense(1),
    ])

    model.compile(
        optimizer=Adam(learning_rate=0.001),
        loss='mean_squared_error')

    lr_callback = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=5, min_lr=1e-6)
    early_stopping = EarlyStopping(monitor='val_loss', patience=10, restore_best_weights=True)

    history = model.fit(
        x_train,
        y_train,
        epochs=50,
        validation_data=(x_val, y_val),
        callbacks=[lr_callback, early_stopping]
    )

    metadata = {
        "model_name": "model_v2",
        "l2": "0.001",
        "dropout": "0.2",
        "architecture": "Dense (512-128-64-1)",
        "optimizer": "adam",
        "loss": "mean_squared_error",
        "epochs": 50,
        "date": str(datetime.now()),
        "training_data_size": len(train_df),
    }

    # Save the model and history, along with the scaler for later use
    training_utils.save(model, history.history, 4, metadata, scaler)


if __name__ == "__main__":
    main()
