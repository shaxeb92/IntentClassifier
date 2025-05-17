# IntentClassifier Android App

## Overview

IntentClassifier is an Android app designed to classify Android applications capable of carrying implicit intent hijacking using a TensorFlow Lite model. The model takes 34 binary features (e.g., permissions, intents) as input and predicts the likelihood of an app being malicious. Users can input features manually or load them from an XML file.

## Features





Manual Input: Enter 34 binary features (0 or 1) in a comma-separated format to classify an app.



XML Input: Load feature vectors from an XML file with <vector> tags containing 34 comma-separated binary values.



Prediction Display: View the prediction result ("Malware" or "Benign") based on the model's output.



Lightweight Model: Uses a TensorFlow Lite model (model.tflite) for efficient on-device inference.

## Requirements





Android Version: Android 5.0 (API 21) or higher.



## Dependencies:





TensorFlow Lite (org.tensorflow:tensorflow-lite:2.16.1)



Jetpack Compose for UI



XML Pull Parser (org.xmlpull:xmlpull:1.1.3.1)

## Project Structure





MainActivity.kt: Main activity handling model loading and UI setup.



PredictionScreen.kt: Jetpack Compose UI for input, prediction, and XML parsing.



assets/model.tflite: TensorFlow Lite model file.


## Dependencies
Add the following dependencies to your `app/build.gradle` file to ensure the app builds and runs correctly:

```gradle
dependencies {
    implementation 'org.tensorflow:tensorflow-lite:2.16.1'
    implementation 'org.xmlpull:xmlpull:1.1.3.1'
    implementation "androidx.activity:activity-compose:1.9.2"
    implementation "androidx.compose.material3:material3:1.3.0"
}
