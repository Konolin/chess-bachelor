import tensorflow as tf

if __name__ == "__main__":
    model = tf.keras.models.load_model('../model/v5/v5-0/v1_model.keras')
    model.export("model_v5")