# JavaTFSavedModelTester
> ### A Java application that can support Google Teachable Machine's SavedModelBundle
## How to use it:
1. On the GUI screen, press the respective button to load the folder that contains the .pb file.
2. Using netron.app, search for the node in the middle of the network that contains the input and output signatures.
3. Example input and output signatures are "serving_default_sequential_4_input" and "StatefulPartitionedCall".
4. Load the model, and using the built-in webcam feature, you can test your model!
5. Code contains a method that shows an example of mapping classes to specific keybinds! An example could be controlling a Snake game.
