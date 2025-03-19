import os
import time
import tensorflow as tf
from tensorflow.keras.layers import (
    Conv2D, BatchNormalization, Dense, Dropout, Input, Concatenate,
    SeparableConv2D, Add, GlobalAveragePooling2D, Multiply, SpatialDropout2D
)
from tensorflow.keras.regularizers import l2
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.optimizers.schedules import CosineDecay
from tensorflow.keras.models import Model
from tensorflow.keras.mixed_precision import set_global_policy
from utils.training_utils import fetch_data_from_db, create_tfrecord_file, create_dataset, save_model_and_history

set_global_policy('mixed_float16')

# Dynamic GPU Memory Growth
gpus = tf.config.list_physical_devices('GPU')
if gpus:
    for gpu in gpus:
        tf.config.experimental.set_memory_growth(gpu, True)

TRAIN_TFRECORD_PATH = "../../data/train.tfrecords"
VAL_TFRECORD_PATH = "../../data/val.tfrecords"

BATCH_SIZE = 256
EPOCHS = 40
VERSION = 11

def squeeze_excite_block(input_tensor, ratio=8):
    filters = input_tensor.shape[-1]
    x = GlobalAveragePooling2D()(input_tensor)
    x = Dense(filters // ratio, activation="swish")(x)
    x = Dense(filters, activation="sigmoid")(x)
    return Multiply()([input_tensor, x])

def residual_block(x, filters):
    shortcut = x
    if x.shape[-1] != filters:
        shortcut = Conv2D(filters, (1, 1), padding='same')(shortcut)
    x = Conv2D(filters, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Conv2D(filters, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = Add()([x, shortcut])
    return x

def build_model():
    board_input = Input(shape=(8, 8, 12), name='board_input')
    x = Conv2D(32, (3, 3), activation='swish', padding='same')(board_input)
    x = BatchNormalization()(x)

    x = residual_block(x, 64)
    x = squeeze_excite_block(x)
    x = SpatialDropout2D(0.2)(x)

    x = SeparableConv2D(64, (3, 3), activation='swish', padding='same')(x)
    x = BatchNormalization()(x)
    x = GlobalAveragePooling2D()(x)

    extra_input = Input(shape=(6,), name='extra_input')
    y = Dense(64, activation='swish')(extra_input)
    y = BatchNormalization()(y)

    combined = Concatenate()([x, y])
    combined = Dense(128, activation='swish', kernel_regularizer=l2(0.003))(combined)
    combined = Dropout(0.2)(combined)
    combined = Dense(32, activation='swish', kernel_regularizer=l2(0.003))(combined)
    combined = Dropout(0.2)(combined)
    output = Dense(1, activation='linear', dtype=tf.float32)(combined)

    lr_schedule = CosineDecay(initial_learning_rate=0.0005, decay_steps=80000, alpha=1e-5)
    optimizer = Adam(learning_rate=lr_schedule)

    model = Model(inputs=[board_input, extra_input], outputs=output)
    model.compile(optimizer=optimizer, loss='mean_squared_error', metrics=['mae'])

    model.summary()
    return model

def main():
    if not os.path.exists(TRAIN_TFRECORD_PATH) or not os.path.exists(VAL_TFRECORD_PATH):
        train_df, val_df = fetch_data_from_db()
        create_tfrecord_file(train_df, TRAIN_TFRECORD_PATH)
        create_tfrecord_file(val_df, VAL_TFRECORD_PATH)

    train_dataset = create_dataset(TRAIN_TFRECORD_PATH, BATCH_SIZE)
    val_dataset = create_dataset(VAL_TFRECORD_PATH, BATCH_SIZE)

    model = build_model()
    history = model.fit(train_dataset, epochs=EPOCHS, validation_data=val_dataset)

    save_model_and_history(model, history, VERSION)

if __name__ == "__main__":
    start_time = time.time()
    main()
    end_time = time.time()
    print(f"Execution time: {(end_time - start_time) / 3600:.2f} hours.")