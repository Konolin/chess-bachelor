#!/usr/bin/env python

from datetime import datetime
import numpy as np
import tensorflow as tf
from tensorflow.keras.layers import Conv2D, BatchNormalization, Flatten, Dense, Dropout, Input, Concatenate
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.models import Model
import chess
import time
import utils.training_utils as training_utils


def encode_extra_features(fen_str):
    """
    Extract extra features from a FEN string.

    Returns:
      A NumPy array of length 6:
      [side_to_move, white kingside, white queenside, black kingside, black queenside, en passant flag]
    """
    board = chess.Board(fen=fen_str)
    side_to_move = 1 if board.turn == chess.BLACK else 0
    castling_rights = [
        1 if board.has_kingside_castling_rights(chess.WHITE) else 0,
        1 if board.has_queenside_castling_rights(chess.WHITE) else 0,
        1 if board.has_kingside_castling_rights(chess.BLACK) else 0,
        1 if board.has_queenside_castling_rights(chess.BLACK) else 0,
    ]
    en_passant_flag = 1 if board.ep_square is not None else 0
    return np.array([side_to_move] + castling_rights + [en_passant_flag], dtype=np.float32)


def load_and_preprocess_data():
    """
    Loads chess positions and converts FEN strings into:
      - Board input: an 8×8×13 one-hot encoded matrix.
      - Extra input: additional chess metadata.

    Returns:
      Tuple: ((x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val)
    """
    print("🔹 Loading data...")
    train_df, val_df = training_utils.fetch_data_lichess()

    print("🔹 Encoding FEN strings for board input...")
    x_board_train = np.stack(train_df['fen'].apply(training_utils.encode_fen_string))
    x_board_val = np.stack(val_df['fen'].apply(training_utils.encode_fen_string))

    print("🔹 Encoding extra features...")
    x_extra_train = np.stack(train_df['fen'].apply(encode_extra_features))
    x_extra_val = np.stack(val_df['fen'].apply(encode_extra_features))

    # Convert target values to NumPy arrays
    y_train = train_df[['evaluation']].values
    y_val = val_df[['evaluation']].values

    print(f"✅ Data loaded. Train samples: {len(train_df)}, Validation samples: {len(val_df)}")
    return (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val


def build_optimized_model():
    """
    Builds an optimized neural network with two input branches:
      - A CNN branch for board representation (3 Conv2D layers).
      - A Dense branch for extra features.

    Returns:
      The compiled Keras model.
    """
    print("🔹 Building optimized model...")

    # Board input branch
    board_input = Input(shape=(8, 8, 13), name='board_input')
    x = Conv2D(64, (5, 5), activation='swish', padding='same')(board_input)
    x = BatchNormalization()(x)
    x = Conv2D(128, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(256, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Flatten()(x)

    # Extra input branch
    extra_input = Input(shape=(6,), name='extra_input')
    y = Dense(128, activation='swish')(extra_input)

    # Combine both branches
    combined = Concatenate()([x, y])
    combined = Dense(256, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.25)(combined)
    combined = Dense(64, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.35)(combined)
    output = Dense(1, activation='linear')(combined)

    model = Model(inputs=[board_input, extra_input], outputs=output)
    model.compile(optimizer=Adam(learning_rate=0.0003), loss='mean_squared_error')
    model.summary()
    print("✅ Optimized model built successfully.")
    return model


def train_model(model, x_train, y_train, x_val, y_val, epochs=50, batch_size=16):
    """
    Trains the model using a generator-based tf.data.Dataset to avoid copying large constant tensors to GPU.

    Args:
      model: The compiled Keras model.
      x_train: Tuple containing training inputs (board and extra features).
      y_train: Training target values.
      x_val: Tuple containing validation inputs.
      y_val: Validation target values.
      epochs: Number of training epochs.
      batch_size: Batch size for training.

    Returns:
      The training history.
    """
    print("🔹 Setting up callbacks...")
    lr_callback = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=4, min_lr=1e-6)
    early_stopping = EarlyStopping(monitor='val_loss', patience=7, restore_best_weights=True)

    print("🔹 Preparing tf.data.Dataset pipelines using generators...")

    # Precompute label shapes to avoid syntax issues
    label_shape_train = (y_train.shape[1],) if len(y_train.shape) > 1 else ()
    label_shape_val = (y_val.shape[1],) if len(y_val.shape) > 1 else ()

    # Generator for training data
    def train_generator():
        for board, extra, label in zip(x_train[0], x_train[1], y_train):
            yield {'board_input': board, 'extra_input': extra}, label

    # Generator for validation data
    def val_generator():
        for board, extra, label in zip(x_val[0], x_val[1], y_val):
            yield {'board_input': board, 'extra_input': extra}, label

    train_dataset = tf.data.Dataset.from_generator(
        train_generator,
        output_signature=(
            {
                'board_input': tf.TensorSpec(shape=(8, 8, 13), dtype=tf.float32),
                'extra_input': tf.TensorSpec(shape=(6,), dtype=tf.float32)
            },
            tf.TensorSpec(shape=label_shape_train, dtype=tf.float32)
        )
    ).batch(batch_size).prefetch(tf.data.AUTOTUNE)

    val_dataset = tf.data.Dataset.from_generator(
        val_generator,
        output_signature=(
            {
                'board_input': tf.TensorSpec(shape=(8, 8, 13), dtype=tf.float32),
                'extra_input': tf.TensorSpec(shape=(6,), dtype=tf.float32)
            },
            tf.TensorSpec(shape=label_shape_val, dtype=tf.float32)
        )
    ).batch(batch_size).prefetch(tf.data.AUTOTUNE)

    print("🔹 Starting training...")
    history = model.fit(
        train_dataset,
        epochs=epochs,
        validation_data=val_dataset,
        callbacks=[lr_callback, early_stopping]
    )

    print("✅ Training complete.")
    return history


def save_model(model, history):
    """
    Saves the trained model along with training history.
    Converts any numpy.float32 values in history to native Python floats to ensure JSON compatibility.
    """
    # Helper function to convert history values to native floats
    def convert_history(hist):
        converted = {}
        for key, values in hist.items():
            converted[key] = [float(val) for val in values]
        return converted

    print("🔹 Saving model...")
    # Convert history before saving
    converted_history = convert_history(history.history)
    training_utils.save(model, converted_history, version=7)
    print("✅ Optimized model saved successfully.")


def main():
    # Load and preprocess data
    (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val = load_and_preprocess_data()

    # Build the model
    model = build_optimized_model()

    # Train the model using the generator-based dataset pipelines
    history = train_model(model, (x_board_train, x_extra_train), y_train, (x_board_val, x_extra_val), y_val)

    # Save the model and training metadata
    save_model(model, history)


if __name__ == "__main__":
    start_time = time.time()

    # Enable dynamic memory growth for available GPUs
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)

    main()

    end_time = time.time()
    print(f"Execution time: {end_time - start_time} seconds")
