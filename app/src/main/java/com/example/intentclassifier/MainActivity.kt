package com.example.intentclassifier

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.Interpreter
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileInputStream
import java.io.InputStream
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
        val fileDescriptor = assets.openFd("model.tflite") // TF Lite model path
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
    val context = LocalContext.current
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var result by remember { mutableStateOf("Result will appear here") }
    var vectors by remember { mutableStateOf(listOf<String>()) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                vectors = parseVectorsFromXml(stream)
            }
        }
    }

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
            label = { Text("Enter 34 binary features or paste here") }, // Updated to 34 features
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val prediction = runPrediction(tflite, inputText.text)
                result = prediction
            }) {
                Text("Predict")
            }

            Button(onClick = { filePickerLauncher.launch("text/xml") }) {
                Text("Load XML")
            }
        }

        Text(
            text = result,
            style = MaterialTheme.typography.bodyLarge
        )

        if (vectors.isNotEmpty()) {
            Text("Select a vector from file:")
            LazyColumn {
                items(vectors.size) { index ->
                    val vector = vectors[index]
                    TextButton(onClick = {
                        inputText = TextFieldValue(vector)
                    }) {
                        Text(vector, maxLines = 1)
                    }
                }
            }
        }
    }
}

fun runPrediction(tflite: Interpreter?, inputStr: String): String {
    return try {
        val tokens = inputStr.split(",")
        if (tokens.size != 34) return "Error: Must contain 34 features" // Updated to 34 features

        val input = FloatArray(34) // Updated to 34 features
        tokens.forEachIndexed { i, v ->
            input[i] = v.trim().toFloat()
        }

        val output = Array(1) { FloatArray(1) }
        tflite?.run(input, output)
        if (output[0][0] > 0.5) "Prediction: Malware" else "Prediction: Benign"
    } catch (e: Exception) {
        "Error: ${e.localizedMessage}"
    }
}

fun parseVectorsFromXml(inputStream: InputStream): List<String> {
    val vectors = mutableListOf<String>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(inputStream, null)

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "vector") {
            val vectorValue = parser.nextText()
            if (vectorValue.split(",").size == 34) { // Updated to 34 features
                vectors.add(vectorValue.trim())
            }
        }
        eventType = parser.next()
    }

    return vectors
}