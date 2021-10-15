package com.specknet.pdiotapp.live

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.specknet.pdiotapp.R
import org.tensorflow.lite.Interpreter

import android.content.res.AssetFileDescriptor
import java.io.IOException
import android.content.res.AssetManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class PredictionActivity : AppCompatActivity() {

    lateinit var input: EditText
    lateinit var output: TextView
    lateinit var button: Button
    lateinit var tflite: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        input = findViewById(R.id.input)
        output = findViewById(R.id.ouput)
        button = findViewById(R.id.button)

        tflite = Interpreter(loadModelFile())

        button.setOnClickListener {
            val prediction = inference(input.text.toString())
            output.text = prediction.toString()
        }
    }

    fun inference(s: String) : Float {
        val inputValue = FloatArray(1)
        inputValue[0] = s.toFloat()

        val inner = FloatArray(1)
        val outputValue: Array<FloatArray> = arrayOf(inner)
        tflite.run(inputValue, outputValue)
        return outputValue[0][0]
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = this.assets.openFd(getModelPath())
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getModelPath(): String {
        return "simple_tensorflow_lite_model.tflite"
    }

    // For later - not in use
    @Throws(IOException::class)
    private fun getLabels(assetManager: AssetManager, labelPath: String): List<String> {
        val labels = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(assetManager.open(labelPath)))
        while (true) {
            val label = reader.readLine() ?: break
            labels.add(label)
        }
        reader.close()
        return labels
    }
}




