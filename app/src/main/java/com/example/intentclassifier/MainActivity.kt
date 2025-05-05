package com.example.intentclassifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : ComponentActivity() {
    private var tflite: Interpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load TFLite model
        try {
            tflite = Interpreter(loadModelFile())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PredictionScreen(tflite)
                }
            }
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite?.close()
    }
}

@Composable
fun PredictionScreen(tflite: Interpreter?) {
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var result by remember { mutableStateOf("Result will appear here") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter 51 binary features (e.g., 0,1,0,...,0)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Button(onClick = {
            result = try {
                val inputStr = inputText.text.split(",")
                if (inputStr.size != 51) {
                    "Error: Please enter 51 binary features"
                } else {
                    val input = FloatArray(51)
                    inputStr.forEachIndexed { index, value ->
                        input[index] = value.trim().toFloat()
                    }

                    val output = Array(1) { FloatArray(1) }
                    tflite?.run(input, output)
                    if (output[0][0] > 0.5) "Prediction: Malware" else "Prediction: Benign"
                }
            } catch (e: Exception) {
                "Error: Invalid input format"
            }
        }) {
            Text("Predict")
        }

        Text(
            text = result,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}