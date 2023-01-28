# MCBC_Eval
[Minecraft](https://www.minecraft.net/) [Fabric](https://fabricmc.net/) 1.19.3 mod featuring live biome classification using the [MCBC Convolutional Neural Network](https://github.com/svenschreiber/mcbc) trained on data created by the [MCBC Data Generator](https://github.com/OfficialLahusa/mcbc_datagen). This project consists of two parts:
- **Java Clientside Fabric Mod (Frontend):** Generates the screenshots and outputs the evaluation results in game.
- **Python CNN (Backend)** Hosts the pretrained CNN and evaluates the generated images.

Both ends communicate using the [XML-RPC Protocol](http://xmlrpc.com/), but are required to run on the same device.

### ⚠️**Please Note:** This repository does not include the pretrained weights of the CNN, only its architecture.
It is necessary to download the [pretrained weights (252 MB)](https://www.dropbox.com/s/9864380t9npznma/mcbc_weights.h5?dl=1) prior to running the Python CNN backend. Save the file under the subpath `/cnn/mcbc_weights.h5` of this workspace.

## Python Dependencies
- Python 3
- keras
- tensorflow
- scikit-image

# Team
Sven Schreiber (https://github.com/svenschreiber) \
Lasse Huber-Saffer (https://github.com/OfficialLahusa) \
Nico Hädicke (https://github.com/Reshxram) \
Lena Kloock
