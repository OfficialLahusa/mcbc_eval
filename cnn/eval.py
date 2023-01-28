import sys
import os
import numpy as np
from xmlrpc.server import SimpleXMLRPCServer
from keras.models import Sequential
from keras.layers import Dense
from keras.layers import Flatten
from keras.layers import Conv2D
from keras.layers import MaxPooling2D
from keras.layers import BatchNormalization
from keras.layers import ReLU
from skimage.io import imread

# Working directory
wkdir = os.path.dirname(os.path.realpath(__file__))

# Model architecture definition
model = Sequential()
model.add(Conv2D(32, (5, 5), padding="same", strides=1, input_shape=(216, 384, 3), activation="relu"))
model.add(MaxPooling2D((2, 2)))
model.add(Conv2D(64, (5, 5), padding="same", strides=1, activation="relu"))
model.add(MaxPooling2D((2, 2)))
model.add(Conv2D(128, (5, 5), padding="same", strides=1, activation="relu"))
model.add(MaxPooling2D((2, 2)))
model.add(Conv2D(256, (5, 5), padding="same", strides=1, activation="relu"))
model.add(MaxPooling2D((2, 2)))
model.add(Flatten())
model.add(Dense(256, use_bias=False))
model.add(BatchNormalization())
model.add(ReLU())
model.add(Dense(5, activation='softmax'))
model.compile(loss='categorical_crossentropy', optimizer='Adam', metrics=['accuracy'])

# Model weights from file
model.load_weights(wkdir + "/mcbc_weights.h5")

# Labels corresponding to the CNN output layer indices
labels = ["aquatic", "arid", "forest", "plains", "snowy"]

with SimpleXMLRPCServer(("localhost", 8000)) as server:
    @server.register_function
    def handle_image(path):
        print(path)

        # Load image and drop alpha channel
        img = imread(path)[:, :, 0:3]
        # Add additional wrapping dimension
        img = np.expand_dims(img, axis=0)

        # Directly call the model to predict the output
        result = model(img, training=False).numpy().tolist()[0]

        # Format the result
        labeled_results = [[e, result[i]] for i, e in enumerate(labels)]
        labeled_results.sort(key=lambda x: x[1], reverse=True)
        result_strs = [f"{e[0]}: {100 * e[1]:.3f}%" for e in labeled_results]
        result_str_concat = " - ".join(result_strs)

        # Print & return
        print(result_str_concat)
        return result_str_concat

    # Run server until [CTRL]+[C] is pressed
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nKeyboard interrupt received, exiting.")
        sys.exit(0)
