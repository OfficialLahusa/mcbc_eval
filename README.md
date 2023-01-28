# MCBC_Eval
[Minecraft](https://www.minecraft.net/) [Fabric](https://fabricmc.net/) 1.19.3 mod featuring live biome classification using the [MCBC Convolutional Neural Network](https://github.com/svenschreiber/mcbc) trained on data created by the [MCBC Data Generator](https://github.com/OfficialLahusa/mcbc_datagen). This project consists of two parts:
- **Java Clientside Fabric Mod (Frontend)**, that generates the screenshots and outputs the evaluation results ingame.
- **Python CNN (Backend)**, that loads the pretrained CNN and evaluates the generated images.

Both ends communicate using the [XML-RPC Protocol](http://xmlrpc.com/), but are both required to run on the same device.
