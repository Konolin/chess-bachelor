import tensorflow as tf
import os

if __name__ == "__main__":
    for device in tf.config.list_physical_devices():
        print(device)
