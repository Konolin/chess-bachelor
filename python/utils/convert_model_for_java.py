import tensorflow as tf

""""
    This script is used to convert the model to a format that can be used in Java.
"""

if __name__ == "__main__":
    model = tf.keras.models.load_model('../model/v9/v9_model.keras')
    model.save("model", save_format="tf")
