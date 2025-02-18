import tensorflow as tf

""""
    This script is used to convert the model to a format that can be used in Java.
"""

if __name__ == "__main__":
    model = tf.keras.models.load_model('../model/v5/v5-0/v1_model.keras')
    model.export("model_v5")