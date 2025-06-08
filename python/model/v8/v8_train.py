import os
import time
import tensorflow as tf

from tensorflow.keras.layers import Conv2D, BatchNormalization, Flatten, Dense, Dropout, Input, Concatenate
from tensorflow.keras.regularizers import l2
from tensorflow.keras.callbacks import ReduceLROnPlateau, EarlyStopping
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.models import Model

from utils.training_utils import fetch_data_from_db, create_tfrecord_file, create_dataset, save_model_and_history

# TFRecord files for training and validation datasets.
TRAIN_TFRECORD_PATH = "../../data/one-hot/train.tfrecords"
VAL_TFRECORD_PATH = "../../data/one-hot/val.tfrecords"

BATCH_SIZE = 64
EPOCHS = 50
VERSION = 8


def build_model():
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


def train_model(model, train_dataset, val_dataset, epochs):
    """
    Trains the model with the training dataset and validates with the validation dataset.
    Callbacks include ReduceLROnPlateau and EarlyStopping.
    """
    # Reduce learning rate on plateau and early stopping callbacks.
    lr_callback = ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=4, min_lr=1e-6)
    early_stopping = EarlyStopping(monitor='val_loss', patience=7, restore_best_weights=True)

    # Train the model.
    history = model.fit(
        train_dataset,
        epochs=epochs,
        validation_data=val_dataset,
        callbacks=[lr_callback, early_stopping]
    )

    return history


def main():
    """
    Main function to train the model.
    """

    # Enable dynamic memory growth for GPUs, if available.
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)

    # Check if TFRecord files exist; if not, fetch data and create them.
    if not os.path.exists(TRAIN_TFRECORD_PATH) or not os.path.exists(VAL_TFRECORD_PATH):
        print("TFRecord files not found. Fetching data from the database and creating TFRecord files...")
        train_df, val_df = fetch_data_from_db()
        create_tfrecord_file(train_df, TRAIN_TFRECORD_PATH)
        create_tfrecord_file(val_df, VAL_TFRECORD_PATH)
    else:
        print("TFRecord files found. Skipping creation and using existing files.")

    # Create streaming datasets from the TFRecord files.
    train_dataset = create_dataset(TRAIN_TFRECORD_PATH, BATCH_SIZE)
    val_dataset = create_dataset(VAL_TFRECORD_PATH, BATCH_SIZE)

    # Build and train the model.
    model = build_model()
    history = train_model(model, train_dataset, val_dataset, EPOCHS)

    # Save the model and training history.
    save_model_and_history(model, history, VERSION)


if __name__ == "__main__":
    start_time = time.time()
    main()
    end_time = time.time()
    print(f"Execution time: {(end_time - start_time) / 3600:.2f} hours.")
