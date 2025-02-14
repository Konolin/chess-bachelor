from datetime import datetime
import numpy as np
import tensorflow as tf
from tensorflow.keras.layers import Conv2D, BatchNormalization, Flatten, Dense, Dropout, Input, Concatenate
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.models import Model
from sklearn.preprocessing import StandardScaler
import utils.training_utils as training_utils
import chess

def encode_extra_features(fen_str):
    """
    Extracts extra features from a FEN string.
    Returns a vector of length 6:
    [side_to_move, white kingside, white queenside, black kingside, black queenside, en passant flag]
    """
    board = chess.Board(fen=fen_str)
    # Feature 1: Side to move (0 for White, 1 for Black)
    side_to_move = 1 if board.turn == chess.BLACK else 0
    # Features 2-5: Castling rights
    castling_rights = [
        1 if board.has_kingside_castling_rights(chess.WHITE) else 0,
        1 if board.has_queenside_castling_rights(chess.WHITE) else 0,
        1 if board.has_kingside_castling_rights(chess.BLACK) else 0,
        1 if board.has_queenside_castling_rights(chess.BLACK) else 0,
    ]
    # Feature 6: En passant availability (1 if available, 0 otherwise)
    en_passant_flag = 1 if board.ep_square is not None else 0
    return np.array([side_to_move] + castling_rights + [en_passant_flag], dtype=np.float32)

def load_and_preprocess_data():
    """
    Loads chess positions from the database and encodes FEN strings into two sets of inputs:
    - Board input: an 8×8×13 one-hot encoded matrix.
    - Extra input: additional features from the FEN string.
    Also scales the target evaluation values (black_score) using StandardScaler.
    """
    print("🔹 Loading data...")
    train_df, val_df = training_utils.fetch_data()

    print("🔹 Encoding FEN strings into board representations...")
    # Using the existing helper from training_utils to get the 8x8x13 board representation
    x_board_train = np.stack(train_df['fen'].apply(training_utils.encode_fen_string))
    x_board_val = np.stack(val_df['fen'].apply(training_utils.encode_fen_string))

    print("🔹 Encoding extra features from FEN strings...")
    x_extra_train = np.stack(train_df['fen'].apply(encode_extra_features))
    x_extra_val = np.stack(val_df['fen'].apply(encode_extra_features))

    print("🔹 Scaling target values (black_score)...")
    scaler = StandardScaler()
    y_train = scaler.fit_transform(train_df[['black_score']])
    y_val = scaler.transform(val_df[['black_score']])

    print(f"✅ Data loaded. Train size: {len(train_df)}, Validation size: {len(val_df)}")
    return (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val, scaler

def build_model_with_extra_features():
    """
    Builds a neural network model that accepts two inputs:
    1. Board input: 8×8×13 tensor processed with convolutional layers.
    2. Extra input: a 6-dimensional vector processed with a small dense network.
    Their outputs are concatenated and passed through additional dense layers to output
    a single scalar evaluation (from Black's point of view).
    """
    print("🔹 Building model with extra input features...")

    # Board branch
    board_input = Input(shape=(8, 8, 13), name='board_input')
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(board_input)
    x = BatchNormalization()(x)
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = BatchNormalization()(x)
    x = Flatten()(x)

    # Extra features branch
    extra_input = Input(shape=(6,), name='extra_input')
    y = Dense(32, activation='relu')(extra_input)

    # Combine both branches
    combined = Concatenate()([x, y])
    combined = Dense(256, activation='relu', kernel_regularizer=l2(0.003))(combined)
    combined = Dropout(0.3)(combined)
    combined = Dense(32, activation='relu')(combined)
    output = Dense(1, activation='linear')(combined)

    model = Model(inputs=[board_input, extra_input], outputs=output)
    model.compile(optimizer=Adam(learning_rate=0.0005), loss='mean_squared_error')
    model.summary()
    print("✅ Model built successfully with extra input features.")
    return model

def train_model(model, x_train, y_train, x_val, y_val):
    """
    Trains the model using ReduceLROnPlateau and EarlyStopping callbacks.
    """
    print("🔹 Setting up callbacks...")
    lr_callback = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=4, min_lr=1e-6)
    early_stopping = EarlyStopping(monitor='val_loss', patience=7, restore_best_weights=True)

    print("🔹 Starting training...")
    history = model.fit(
        {'board_input': x_train[0], 'extra_input': x_train[1]},
        y_train,
        epochs=50,
        validation_data=({'board_input': x_val[0], 'extra_input': x_val[1]}, y_val),
        callbacks=[lr_callback, early_stopping]
    )

    print("✅ Training complete.")
    return history

def save_model(model, history, scaler):
    """
    Saves the trained model, training history, metadata, and scaler.
    """
    metadata = {
        "model_name": "model_with_extra_input_v1",
        "architecture": "Board: Conv2D(32)-BatchNorm-Conv2D(64)-BatchNorm-Flatten; Extra: Dense(32); Combined: Dense(256)-Dropout(0.25)-Dense(32)-Dense(1)",
        "optimizer": "adam",
        "loss": "mean_squared_error",
        "epochs": len(history.history['loss']),
        "date": str(datetime.now())
    }

    print("🔹 Saving model and metadata...")
    training_utils.save(model, history.history, version=1, metadata=metadata, scaler=scaler)
    print("✅ Model saved successfully.")

def main():
    """
    Main function to load data, build the model with extra features, train it, and save the results.
    """
    (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val, scaler = load_and_preprocess_data()
    model = build_model_with_extra_features()
    history = train_model(model, (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val)
    save_model(model, history, scaler)

if __name__ == "__main__":
    main()
