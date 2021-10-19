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
import java.time.Instant
import kotlin.collections.ArrayList



class PredictionActivity : AppCompatActivity() {

    //Joe: BlueTooth variables
    var count = 0
    var windowsize = 50
    var accXList = FloatArray(windowsize)
    var accYList = FloatArray(windowsize)
    var accZList = FloatArray(windowsize)

    var gyrXList = FloatArray(windowsize)
    var gyrYList = FloatArray(windowsize)
    var gyrZList = FloatArray(windowsize)

    lateinit var input: EditText
    lateinit var output: TextView
    lateinit var button: Button
    lateinit var current_activity : TextView
    lateinit var tflite: Interpreter

    // global broadcast receiver so we can unregister it
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    private var labels : Array<String> = emptyArray()
    // standing 0->3, walking 1->8
    // N.B. when we classify all classes, idxs array will not be needed
    private val idxs = arrayOf(7,9)
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

        input = findViewById(R.id.input)
        output = findViewById(R.id.ouput)
        button = findViewById(R.id.button)
        current_activity = findViewById(R.id.current_activity)

        tflite = Interpreter(loadModelFile())

        val color = arrayOf(Color.RED, Color.GREEN)
        var i = 0

        var lastUpdate = System.currentTimeMillis()

        button.setOnClickListener {
            val test_instance = readTestInstance()
            val prediction = inference(test_instance)

            // TODO what happens if null?
            val max_prob = prediction.maxOrNull()
            val max_idx = prediction.asList().indexOf(max_prob)

            output.text = max_prob.toString()
            current_activity.text = labels[idxs[max_idx]]

            activity_icon.setImageResource(icons[idxs[max_idx]])
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

                    //add to data table
                    accXList[count] = accx
                    accYList[count] = accy
                    accZList[count] = accz

                    gyrXList[count] = gyrx
                    gyrYList[count] = gyry
                    gyrZList[count] = gyrz

                    count += 1

                    if (count == windowsize) {
                        var arr =
                            arrayOf(accXList, accYList, accZList, gyrXList, gyrYList, gyrZList)
                        runOnUiThread {
                            val prediction = inference(arr)

                            // TODO what happens if null?
                            val max_prob = prediction.maxOrNull()
                            val max_idx = prediction.asList().indexOf(max_prob)

                            output.text = prediction[0].toString() + " " + prediction[1].toString()
                            current_activity.text = labels[idxs[max_idx]]

                            activity_icon.setImageResource(icons[idxs[max_idx]])

                            button.setBackgroundColor(color[i]);
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
                        accXList = FloatArray(windowsize)
                        accYList = FloatArray(windowsize)
                        accZList = FloatArray(windowsize)

                        gyrXList = FloatArray(windowsize)
                        gyrYList = FloatArray(windowsize)
                        gyrZList = FloatArray(windowsize)
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
    }

    fun inference(input: Array<FloatArray>) : FloatArray {
        val inner = FloatArray(2)
        val outputValue: Array<FloatArray> = arrayOf(inner)
        tflite.run(input, outputValue)
        return outputValue[0]
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = this.assets.openFd(getModelPath())
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getModelPath(): String {
        return "cnn_lyingback_running.tflite"
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
        val reader = BufferedReader(InputStreamReader(assets.open("test_instance0.txt")))
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