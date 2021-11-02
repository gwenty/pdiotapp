package com.specknet.pdiotapp.live

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
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import java.time.Instant
import kotlin.collections.ArrayList



class PredictionActivity : AppCompatActivity() {

    //Joe: BlueTooth variables
    var count = 0
    var countThingy = 0
    var windowsize = 20
    var n_classes = 18

    val updateButtonColor = arrayOf(Color.RED, Color.GREEN)
    lateinit var respeckPrediction: FloatArray
    lateinit var thingyPrediction: FloatArray

    var arr = Array(windowsize) { FloatArray(6) }

    lateinit var input: EditText
    lateinit var output: TextView
    lateinit var button: Button
    lateinit var current_activity : TextView
    lateinit var respeckClassifier: Interpreter
    lateinit var thingyClassifier: Interpreter

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    lateinit var thingyLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperThingy: Looper
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    private var labels : Array<String> = emptyArray()
    // standing 0->3, walking 1->8
    // N.B. when we classify all classes, idxs array will not be needed
    private val idxs = arrayOf(3,8)
    private val icons = arrayOf(R.drawable.sitting,
                                R.drawable.sitting,
                                R.drawable.sitting,
                                R.drawable.standing,
                                R.drawable.lying,
                                R.drawable.lying,
                                R.drawable.lying,
                                R.drawable.lying,
                                R.drawable.walking,
                                R.drawable.running,
                                R.drawable.stairs,
                                R.drawable.stairs,
                                R.drawable.desk,
                                R.drawable.movement,
                                R.drawable.falling,
                                R.drawable.falling,
                                R.drawable.falling,
                                R.drawable.falling)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        val res: Resources = resources
        labels = res.getStringArray( R.array.activity_types )

        output = findViewById(R.id.ouput)
        button = findViewById(R.id.button)
        current_activity = findViewById(R.id.current_activity)

        respeckClassifier = Interpreter(loadModelFile(getRespeckModelPath()))
        thingyClassifier = Interpreter(loadModelFile(getThingyModelPath()))

        thingyPrediction = FloatArray(n_classes)
        respeckPrediction = FloatArray(n_classes)

        var i = 0

        var lastUpdate = System.currentTimeMillis()

        button.setOnClickListener {
            val test_instance = readTestInstance()
            val respeckPrediction = inference(test_instance, respeckClassifier)
            val thingyPrediction = inference(test_instance, thingyClassifier)

            // TODO what happens if null?
            val prediction = sumPredictions(respeckPrediction, thingyPrediction)
            val max_prob = prediction.maxOrNull()
            val max_idx = prediction.asList().indexOf(max_prob)

            output.text = max_prob.toString()
            // current_activity.text = labels[idxs[max_idx]]
            current_activity.text = labels[max_idx]

            // activity_icon.setImageResource(icons[idxs[max_idx]])
            activity_icon.setImageResource(icons[max_idx])
        }

        //Joe: initialising the respeck Receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val accx = liveData.accelX
                    val accy = liveData.accelY
                    val accz = liveData.accelZ

                    val gyrx = liveData.gyro.x
                    val gyry = liveData.gyro.y
                    val gyrz = liveData.gyro.z

                    arr[count] = floatArrayOf(accx,accy,accz,gyrx,gyry,gyrz)


                    count += 1

                    if (count == windowsize) {
                        runOnUiThread {
                            respeckPrediction = inference(arr, respeckClassifier)
                            updatePrediction(i)

                            if (i==0) {
                                i = 1
                            }
                            else if (i==1) {
                                i = 0
                            }

                            time.text = ((System.currentTimeMillis() - lastUpdate)).toString()
                            lastUpdate = System.currentTimeMillis()
                        }

                        count = 0
                    }
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        // set up the broadcast receiver
        thingyLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val accx = liveData.accelX
                    val accy = liveData.accelY
                    val accz = liveData.accelZ

                    val gyrx = liveData.gyro.x
                    val gyry = liveData.gyro.y
                    val gyrz = liveData.gyro.z

                    arr[countThingy] = floatArrayOf(accx,accy,accz,gyrx,gyry,gyrz)


                    countThingy += 1

                    if (countThingy == windowsize) {
                        runOnUiThread {
                            thingyPrediction = inference(arr, thingyClassifier)
                            updatePrediction(i)

                            if (i == 0) {
                                i = 1
                            } else if (i == 1) {
                                i = 0
                            }

                            time.text = ((System.currentTimeMillis() - lastUpdate)).toString()
                            lastUpdate = System.currentTimeMillis()
                        }

                        countThingy = 0
                    }
                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy = HandlerThread("bgThreadThingyLive")
        handlerThreadThingy.start()
        looperThingy = handlerThreadThingy.looper
        val handlerThingy = Handler(looperThingy)
        this.registerReceiver(thingyLiveUpdateReceiver, filterTestThingy, null, handlerThingy)
    }

    private fun sumPredictions(prediction1 : FloatArray, prediction2 : FloatArray) : FloatArray{
        val meanPrediction = FloatArray(prediction1.size)
        for (i in 0 until prediction1.size) {
            meanPrediction[i] = ( prediction1[i] + prediction2[i] ) / 2
        }
        return meanPrediction
    }

    fun inference(input: Array<FloatArray>, classifier: Interpreter) : FloatArray {
        val inner = FloatArray(n_classes)
        val outputValue: Array<FloatArray> = arrayOf(inner)
        classifier.run(input, outputValue)
        return outputValue[0]
    }

    private fun updatePrediction(i : Int) {

        // If both predictions are available, combine. Otherwise use only existing
        lateinit var prediction : FloatArray
        if (thingyPrediction.sum() == 0f)
            prediction = respeckPrediction
        else if (respeckPrediction.sum() == 0f)
            prediction = thingyPrediction
        else
            prediction = sumPredictions(respeckPrediction, thingyPrediction)

        // TODO what happens if null?
        val max_prob = prediction.maxOrNull()
        val max_idx = prediction.asList().indexOf(max_prob)

        output.text = max_prob.toString()
        //current_activity.text = labels[idxs[max_idx]]
        current_activity.text = labels[max_idx]

        //activity_icon.setImageResource(icons[idxs[max_idx]])
        activity_icon.setImageResource(icons[max_idx])

        button.setBackgroundColor(updateButtonColor[i])
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(modelPath : String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = this.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getRespeckModelPath(): String {
        return "cnn_simple_full.tflite"
    }

    private fun getThingyModelPath(): String {
        return "cnn_simple_full_thingy.tflite"
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
        val rows = windowsize
        val cols = 6
        val reader = BufferedReader(InputStreamReader(assets.open("test_instance_window20_0.txt")))
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