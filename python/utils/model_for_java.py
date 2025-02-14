import tensorflow as tf

if __name__ == "__main__":
    model = tf.keras.models.load_model('../model/v4/v4_model.keras')
    model.export("model_v4")