# mlmodeldeployment
Testing of Different Models for Android Studio. Select your pretrained neural network, select or capture your image, then find results.

Credit to https://www.youtube.com/watch?v=tySgZ1rEbW4 for UI development

Regnety model from Kaggle
[https://www.kaggle.com/models/adityakane/regnety](url)

Choose Your LLM, then classify image based on model (Traditional vs. Non-Traditional Image Classification).

![Screenshot from 2024-07-22 16-48-41](https://github.com/user-attachments/assets/cbb0b01f-42f3-4b49-9e74-b543cdf56942)

Note: the resnet-50 model is not included in the repo since it exceeds Github's file size limit. The model can be loaded from https://www.kaggle.com/models/tensorflow/resnet-v2, making sure to download the with metadata file. To insert the file into Android Studio, first make sure your project is in "Android View". Then right click on App, select new, select other, then select Tensorflow Lite model. Android studio will take the TF lite model and populate in the correct folder. Import statements may have to be changed in the classifyResnet() method in order for the model to perform proper classification.
