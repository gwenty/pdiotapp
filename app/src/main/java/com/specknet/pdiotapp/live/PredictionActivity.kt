package com.specknet.pdiotapp.live

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.specknet.pdiotapp.R
import org.tensorflow.lite.Interpreter

import android.content.res.AssetFileDescriptor
import java.io.IOException
import android.content.res.AssetManager
import android.content.res.Resources
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_prediction.*
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class PredictionActivity : AppCompatActivity() {

    lateinit var input: EditText
    lateinit var output: TextView
    lateinit var button: Button
    lateinit var current_activity : TextView
    lateinit var tflite: Interpreter

    private var labels : Array<String> = emptyArray()
    // standing 0->3, walking 1->8
    private val idxs = arrayOf(3,8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        val res: Resources = resources
        labels = res.getStringArray( R.array.activity_types ) ;

        input = findViewById(R.id.input)
        output = findViewById(R.id.ouput)
        button = findViewById(R.id.button)
        current_activity = findViewById(R.id.current_activity)

        tflite = Interpreter(loadModelFile())

        button.setOnClickListener {
            val test_instance = readTestInstance()
            val prediction = inference(test_instance)

            // TODO what happens if null?
            val max_prob = prediction.maxOrNull()
            val max_idx = prediction.asList().indexOf(max_prob)
            
            output.text = max_prob.toString()
            current_activity.text = labels[idxs[max_idx]]
        }
    }

    fun inference(input: Array<FloatArray>) : FloatArray {
        val inner = FloatArray(2)
        val outputValue: Array<FloatArray> = arrayOf(inner)
        tflite.run(input, outputValue)
        return outputValue[0]
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
        return "cnn_walking_standing.tflite"
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

    private fun readTestInstance(): Array<FloatArray> {
        val rows = 50
        val cols = 6
        val reader = BufferedReader(InputStreamReader(assets.open("test_instance15.txt")))
        var counter = 0
        val test_instance = Array(rows) { FloatArray(cols) }
        while (true) {
            val line = reader.readLine() ?: break
            val split: List<String> = line.split("\\s".toRegex())
            val farray = FloatArray(cols)
            for (i in 0..cols-1) {
                farray[i] = split[i].toFloat()
            }
            test_instance[counter] = farray
            counter ++
        }
        reader.close()
        return test_instance
    }
}




