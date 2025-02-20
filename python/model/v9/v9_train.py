import os
import time
import tensorflow as tf
from tensorflow.keras.layers import (Conv2D, BatchNormalization, Flatten, Dense, Dropout, Input, Concatenate,
                                     SeparableConv2D, Add, GlobalAveragePooling2D, Multiply)
from tensorflow.keras.regularizers import l2
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.optimizers.schedules import CosineDecay
from tensorflow.keras.models import Model
from tensorflow.keras.mixed_precision import set_global_policy
from utils.training_utils import fetch_data_from_db, create_tfrecord_file, create_dataset, save_model_and_history

# Enable Mixed Precision for Speed Boost 🚀
set_global_policy('mixed_float16')

# Enable Dynamic GPU Memory Growth
gpus = tf.config.list_physical_devices('GPU')
if gpus:
    for gpu in gpus:
        tf.config.experimental.set_memory_growth(gpu, True)

# Paths for TFRecord datasets
TRAIN_TFRECORD_PATH = "../../data/train.tfrecords"
VAL_TFRECORD_PATH = "../../data/val.tfrecords"

BATCH_SIZE = 128  # Increased batch size for efficiency
EPOCHS = 50
VERSION = 9

def squeeze_excite_block(input_tensor, ratio=16):
    filters = input_tensor.shape[-1]
    x = GlobalAveragePooling2D()(input_tensor)
    x = Dense(filters // ratio, activation="swish")(x)
    x = Dense(filters, activation="sigmoid")(x)
    return Multiply()([input_tensor, x])

def residual_block(x, filters):
    shortcut = x
    # Ensure shortcut has the same number of filters
    if x.shape[-1] != filters:
        shortcut = Conv2D(filters, (1, 1), padding='same')(shortcut)  # Fix shape mismatch
    x = Conv2D(filters, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(filters, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Add()([x, shortcut])  # Add residual connection
    return x

def build_model():
    """
    Optimized neural network with:
      - A CNN branch featuring residual connections, squeeze-and-excite blocks,
        and global pooling for robust feature extraction.
      - An enhanced Dense branch for extra features with additional normalization.
      - Cosine decay learning rate schedule and an optimizer that uses decoupled weight decay.
    """
    # Board (CNN) branch
    board_input = Input(shape=(8, 8, 13), name='board_input')
    x = Conv2D(64, (3, 3), activation='swish', padding='same')(board_input)
    x = BatchNormalization()(x)
    x = residual_block(x, 128)
    x = squeeze_excite_block(x)
    x = SeparableConv2D(128, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    # Instead of Flatten, we use GlobalAveragePooling2D for better spatial aggregation.
    x = GlobalAveragePooling2D()(x)

    # Extra features branch
    extra_input = Input(shape=(6,), name='extra_input')
    y = Dense(128, activation='swish')(extra_input)
    y = BatchNormalization()(y)  # Added normalization to extra branch
    y = Dense(64, activation='swish')(y)
    y = BatchNormalization()(y)

    # Combine both branches
    combined = Concatenate()([x, y])
    combined = Dense(256, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.25)(combined)
    combined = Dense(64, activation='swish', kernel_regularizer=l2(0.005))(combined)
    combined = Dropout(0.25)(combined)
    output = Dense(1, activation='linear', dtype=tf.float32)(combined)

    # Cosine Decay Learning Rate Schedule
    lr_schedule = CosineDecay(initial_learning_rate=0.0003, decay_steps=100000, alpha=0.00005)

    # Use AdamW if available for decoupled weight decay; fall back to Adam if not.
    try:
        optimizer = Adam(learning_rate=lr_schedule, weight_decay=1e-4)
    except Exception as e:
        print("AdamW not available, falling back to Adam. Exception:", e)
        optimizer = Adam(learning_rate=lr_schedule)

    # Compile the model with an additional MAE metric for monitoring.
    model = Model(inputs=[board_input, extra_input], outputs=output)
    model.compile(optimizer=optimizer, loss='mean_squared_error', metrics=['mae'])

    model.summary()
    return model

def main():
    """
    Main function to train the model.
    """
    # Check for existing TFRecords; if missing, create them.
    if not os.path.exists(TRAIN_TFRECORD_PATH) or not os.path.exists(VAL_TFRECORD_PATH):
        print("TFRecord files not found. Fetching data...")
        train_df, val_df = fetch_data_from_db()
        create_tfrecord_file(train_df, TRAIN_TFRECORD_PATH)
        create_tfrecord_file(val_df, VAL_TFRECORD_PATH)

    # Load streaming datasets with an optimized pipeline.
    train_dataset = create_dataset(TRAIN_TFRECORD_PATH, BATCH_SIZE)
    val_dataset = create_dataset(VAL_TFRECORD_PATH, BATCH_SIZE)

    # Build & Train the model.
    model = build_model()
    history = model.fit(train_dataset, epochs=EPOCHS, validation_data=val_dataset)

    # Save the model and training history.
    save_model_and_history(model, history, VERSION)

if __name__ == "__main__":
    start_time = time.time()
    main()
    end_time = time.time()
    print(f"Execution time: {(end_time - start_time) / 3600:.2f} hours.")
