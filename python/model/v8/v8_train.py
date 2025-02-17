#!/usr/bin/env python
import os
import json
import time
import numpy as np
import tensorflow as tf
import pandas as pd
import chess
import mysql.connector
from dotenv import load_dotenv
import matplotlib.pyplot as plt

from tensorflow.keras.layers import Conv2D, BatchNormalization, Flatten, Dense, Dropout, Input, Concatenate
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.models import Model

from utils.training_utils import fetch_data_lichess, create_tfrecord_file, create_dataset, save_model_and_history


# -----------------------------------------------------------------------------
# Define the optimized model.
# -----------------------------------------------------------------------------
def build_optimized_model():
    """
    Builds and compiles the neural network with two input branches:
      - A CNN branch for board representation.
      - A Dense branch for extra features.
    """
    board_input = Input(shape=(8, 8, 13), name='board_input')
    x = Conv2D(64, (5, 5), activation='swish', padding='same')(board_input)
    x = BatchNormalization()(x)
    x = Conv2D(128, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(256, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Flatten()(x)

    extra_input = Input(shape=(6,), name='extra_input')
    y = Dense(128, activation='swish')(extra_input)

    combined = Concatenate()([x, y])
    combined = Dense(256, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.25)(combined)
    combined = Dense(64, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.35)(combined)
    output = Dense(1, activation='linear')(combined)

    model = Model(inputs=[board_input, extra_input], outputs=output)
    model.compile(optimizer=Adam(learning_rate=0.0003), loss='mean_squared_error')
    model.summary()
    return model


# -----------------------------------------------------------------------------
# Training and saving functions.
# -----------------------------------------------------------------------------
def train_model(model, train_dataset, val_dataset, epochs=50):
    lr_callback = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=4, min_lr=1e-6)
    early_stopping = EarlyStopping(monitor='val_loss', patience=7, restore_best_weights=True)
    history = model.fit(
        train_dataset,
        epochs=epochs,
        validation_data=val_dataset,
        callbacks=[lr_callback, early_stopping]
    )
    return history


# -----------------------------------------------------------------------------
# Main function: Checks for TFRecord files, creates datasets, builds model, trains, and saves.
# -----------------------------------------------------------------------------
def main():
    # Enable dynamic memory growth for GPUs, if available.
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)

    # Check if TFRecord files exist; if not, fetch data and create them.
    train_tfrecord = "../../data/train.tfrecords"
    val_tfrecord = "../../data/val.tfrecords"

    if not os.path.exists(train_tfrecord) or not os.path.exists(val_tfrecord):
        print("🔹 TFRecord files not found. Fetching data from the database and creating TFRecord files...")
        train_df, val_df = fetch_data_lichess()
        create_tfrecord_file(train_df, train_tfrecord)
        create_tfrecord_file(val_df, val_tfrecord)
    else:
        print("🔹 TFRecord files found. Skipping creation and using existing files.")

    # Create streaming datasets from the TFRecord files.
    batch_size = 64
    train_dataset = create_dataset(train_tfrecord, batch_size)
    val_dataset = create_dataset(val_tfrecord, batch_size)

    # Build and train the model.
    model = build_optimized_model()
    history = train_model(model, train_dataset, val_dataset, epochs=50)

    # Save the model and training history.
    save_model_and_history(model, history, version=8)


if __name__ == "__main__":
    start_time = time.time()
    main()
    end_time = time.time()
    print(f"Execution time: {end_time - start_time:.2f} seconds")
