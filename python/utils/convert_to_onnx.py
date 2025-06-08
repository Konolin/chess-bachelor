import tensorflow as tf
import tf2onnx

if __name__ == '__main__':
    # Temporarily disable mixed precision
    tf.keras.mixed_precision.set_global_policy('float32')

    # Load your Keras model
    model = tf.keras.models.load_model('../model/v10/v10_model.keras')

    # Define input signature for ONNX
    spec = (tf.TensorSpec((None, 8, 8, 12), tf.float32, name="board_input"),
            tf.TensorSpec((None, 6), tf.float32, name="extra_input"))

    # Convert the model to ONNX format and save directly
    onnx_model, _ = tf2onnx.convert.from_keras(
        model, input_signature=spec, opset=17, output_path='model.onnx'
    )

    print("ONNX model exported successfully.")
