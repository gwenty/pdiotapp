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
import android.widget.Toast
import com.google.firebase.firestore.ktx.firestore
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlin.collections.ArrayList
import com.google.gson.Gson
import java.net.URLEncoder

//Joe: new Imports for queue

//Joe: Imports for firebase
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.UnaryOperator
import kotlin.collections.HashMap


//This is our self implemented prediction activity.
class PredictionActivity : AppCompatActivity() {

    //Joe: BlueTooth variables
    var count = 0
    var countThingy = 0
    var windowsize = 25
    var n_classes = 18

    //Joe: data store variables
    var predictionList : ArrayList<Int> = ArrayList()
    var userEmailGlob = " "



    var checkTime = 20
    //Joe: continuous window variables
    //initialising the queue here :)
    val contQueueRespeck: Queue<FloatArray> = LinkedList<FloatArray>()
    val contQueueThingy: Queue<FloatArray> = LinkedList<FloatArray>()
    //Joe: sync time varibales
    var timePassed = 0L

    val updateButtonColor = arrayOf(Color.RED, Color.GREEN)
    lateinit var respeckPrediction: FloatArray
    lateinit var thingyPrediction: FloatArray
    var previousClass: Int = -1

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

    // For grouping ino subsets
    private var subset_labels = arrayOf("Sitting/standing", "Walking", "Running", "Lying Down", "Falling")
    val sitting_activities = arrayOf(0,1,2,3,12)
    val walking_activities = arrayOf(8,10,11,13)
    val running_activities = arrayOf(9)
    val lying_activities = arrayOf(4,5,6,7)
    val falling_activities = arrayOf(14,15,16,17)
    val subset_ixs = arrayOf(0,0,0,0,3,3,3,3,1,2,1,1,0,1,4,4,4,4)
    val subset_activities = arrayOf(sitting_activities, walking_activities, running_activities, lying_activities, falling_activities)

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

    // Stuff for state machine
    private val sitting = arrayListOf(1F,1F,1F,1F,0F,0F,0F,0F,0F,0F,0F,0F,1F,1F,1F,1F,1F,1F)
    private val sitting_forward = arrayListOf(1F,1F,1F,1F,0F,0F,0F,0F,0F,0F,0F,0F,1F,1F,1F,1F,1F,1F)
    private val sitting_backward = arrayListOf(1F,1F,1F,1F,0F,0F,0F,0F,0F,0F,0F,0F,1F,1F,1F,1F,1F,1F)
    private val standing = arrayListOf(1F,1F,1F,1F,0F,0F,0F,0F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val lying_left = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val lying_right = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val lying_stomach = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val lying_back = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val walking = arrayListOf(0F,0F,0F,1F,0F,0F,0F,0F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val running = arrayListOf(0F,0F,0F,1F,0F,0F,0F,0F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val climbing_stairs = arrayListOf(0F,0F,0F,1F,0F,0F,0F,0F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val descending_stairs = arrayListOf(0F,0F,0F,1F,0F,0F,0F,0F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val desk_work = arrayListOf(1F,1F,1F,1F,0F,0F,0F,0F,0F,0F,0F,0F,1F,1F,1F,1F,1F,1F)
    private val movement = arrayListOf(1F,1F,1F,1F,1F,1F,1F,1F,1F,1F,1F,1F,0F,1F,1F,1F,1F,1F)
    private val falling_knees = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val falling_back = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val falling_left = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)
    private val falling_right = arrayListOf(0F,0F,0F,0F,1F,1F,1F,1F,0F,0F,0F,0F,0F,1F,0F,0F,0F,0F)

    private val state_machine = arrayListOf(sitting, sitting_forward, sitting_backward,
            standing, lying_left, lying_right, lying_stomach, lying_back,
            walking, running, climbing_stairs, descending_stairs, desk_work,
            movement, falling_knees, falling_back, falling_left, falling_right)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        var userEmail = intent.getStringExtra("email_id")
        userEmailGlob = userEmail!!

        val res: Resources = resources
        labels = res.getStringArray( R.array.activity_types )

        output = findViewById(R.id.ouput)
        button = findViewById(R.id.button)
        current_activity = findViewById(R.id.current_activity)

        respeckClassifier = Interpreter(loadModelFile(getRespeckModelPath()))
        thingyClassifier = Interpreter(loadModelFile(getThingyModelPath()))

        thingyPrediction = FloatArray(n_classes)
        respeckPrediction = FloatArray(n_classes)

        // Set the penalty in the state machine
        for (p in 0 until state_machine.size) {
            for (s in 0 until state_machine[p].size) {
                if (state_machine[p][s] == 0F) {
                    state_machine[p][s] = 1F
                }
            }
        }

        var i = 0

        var lastUpdate = System.currentTimeMillis()

        btn_data_save.setOnClickListener {
            saveData()
        }


        button.setOnClickListener {
            val test_instance = readTestInstance()
            //val respeckPrediction = inference(test_instance, respeckClassifier)
            //val thingyPrediction = inference(test_instance, thingyClassifier)

            val thread = Thread {
                try {
                    var prediction = sendGet(readTestInstance(), "thingy_prediction")
                    Log.i("response", prediction.toString())
                    if (prediction != null) {
                        runOnUiThread {
                            // change UI elements here
                            updatePredictionOutput(prediction)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            thread.start()
        }

        //Joe: initialising the respeck Receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)
                //runOnUiThread {
                //    time.text = timePassed.toString()
                //}

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

                    //Joe: adding new  data to the queue
                    contQueueRespeck.add(floatArrayOf(accx,accy,accz,gyrx,gyry,gyrz))

                    //If queue > window size then we need to drop the oldest piece of data
                    while (contQueueRespeck.size > windowsize) {
                        contQueueRespeck.remove()
                    }

                    count +=1

                    timePassed = System.currentTimeMillis() - lastUpdate
                    //if (contQueueRespeck.size == windowsize && count >= checkTime && timePassed > 1000) {
                    if (contQueueRespeck.size == windowsize && timePassed > 1000) {
                        lastUpdate = System.currentTimeMillis()
                        var collected_instance = contQueueRespeck.toTypedArray()

                        val thread = Thread {
                            try {
                                respeckPrediction = sendGet(collected_instance, "thingy_prediction")
                                if (respeckPrediction != null) {
                                    runOnUiThread {
                                        // change UI elements here
                                        updatePrediction(i)
                                        //time.text = timePassed.toString()//((System.currentTimeMillis() - lastUpdate)).toString()

                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        thread.start()

                        /* To run prediction locally
                        runOnUiThread {
                            //Converting the queue to an array.
                            respeckPrediction = inference(contQueueRespeck.toTypedArray(), respeckClassifier)
                            updatePrediction(i)

                            time.text = ((System.currentTimeMillis() - lastUpdate)).toString()
                            lastUpdate = System.currentTimeMillis()
                        }*/

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

                    //Joe: adding new  data to the queue
                    contQueueThingy.add(floatArrayOf(accx,accy,accz,gyrx,gyry,gyrz))

                    //If queue > window size then we need to drop the oldest piece of data
                    while (contQueueThingy.size > windowsize) {
                        contQueueThingy.remove()
                    }

                    countThingy +=1
                    timePassed = System.currentTimeMillis() - lastUpdate

                    //if (contQueueThingy.size == windowsize && countThingy >= checkTime && timePassed > 1000) {
                    if (contQueueThingy.size == windowsize && timePassed > 1000) {
                        lastUpdate = System.currentTimeMillis()
                        var collected_instance = contQueueThingy.toTypedArray()

                        var res_inst = contQueueRespeck.toTypedArray()


                        val thread = Thread {
                            try {
                                if (res_inst.size == windowsize)
                                {
                                    respeckPrediction = sendGet(res_inst, "respeck_prediction")
                                }


                                thingyPrediction = sendGet(collected_instance, "thingy_prediction")
                                if (thingyPrediction != null) {
                                    runOnUiThread {
                                        // change UI elements here
                                        updatePrediction(i)
                                        //time.text = timePassed.toString()//((System.currentTimeMillis() - lastUpdate)).toString()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        thread.start()

                        /* //The run prediction locally
                        runOnUiThread {
                            //Converting the queue to an array.
                            thingyPrediction = inference(contQueueThingy.toTypedArray(), thingyClassifier)
                            updatePrediction(i)

                            time.text = ((System.currentTimeMillis() - lastUpdate)).toString()
                            lastUpdate = System.currentTimeMillis()
                        }*/

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

    private fun normalise(prediction : FloatArray) : FloatArray{
        val sum = prediction.sum()
        for (i in prediction.indices) {
            prediction[i] = prediction[i] / sum
        }
        return prediction
    }

    private fun groupPrediction(prediction : FloatArray, subset : Int) : Float {
        // Sum over that group
        var subset_prediction = 0F
        for (activity in subset_activities[subset].indices)
            subset_prediction += prediction[subset_activities[subset][activity]]

        return subset_prediction
    }

    private fun updatePredictionOutput(prediction : FloatArray) {
        //val max_prob = prediction.maxOrNull()
        //val max_idx = prediction.asList().indexOf(max_prob)
        var weighted_prediction: FloatArray = FloatArray(n_classes)

        if (previousClass != -1)
            // Multiply the predicted probabilities with the transitions from the previous class
            for (i in 0 until n_classes)
                weighted_prediction[i] = state_machine[previousClass][i] * prediction[i]
        else
            weighted_prediction = prediction
        weighted_prediction = normalise(weighted_prediction)

        val max_prob = weighted_prediction.maxOrNull()
        val max_idx = weighted_prediction.asList().indexOf(max_prob)
        previousClass = max_idx

        val subset = subset_ixs[max_idx]
        val subset_prediction = groupPrediction(weighted_prediction, subset)

        // current_activity.text = labels[idxs[max_idx]]
        output.text = labels[max_idx] + " (" + "%.2f".format(max_prob) + ")"
        current_activity.text = subset_labels[subset] + " (" + "%.2f".format(subset_prediction) + ")"

        // activity_icon.setImageResource(icons[idxs[max_idx]])
        activity_icon.setImageResource(icons[max_idx])

        //Joe: adding the prediction to the list
        predictionList.add(max_idx)
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

        updatePredictionOutput(prediction)

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

    fun sendGet(test_instance_array : Array<FloatArray>, url : String): FloatArray {
        var test_instance = Gson().toJson(test_instance_array)
        var params = URLEncoder.encode("instance", "UTF-8") + "=" + URLEncoder.encode(
            test_instance, "UTF-8")

        Log.i("requests","Sending request")
        val response = URL("http://pdiot.eu.pythonanywhere.com/"+ url + "?"+params).readText()
        Log.i("requests",response)

        var map: Map<String, ArrayList<ArrayList<Double>>> = HashMap()
        map = Gson().fromJson(response, map.javaClass)
        var pred : ArrayList<Double>? = map["prediction"]?.get(0)

        val floatArray = FloatArray(n_classes)
        var i = 0

        if (pred != null) {
            for (f in pred) {
                floatArray[i++] = f.toFloat() ?: Float.NaN // Or whatever default you want.
            }
        }

        return floatArray
    }



    private fun getRespeckModelPath(): String {
        return "cnn_simple_full.tflite"
    }

    private fun getThingyModelPath(): String {
        return "cnn_simple_full_thingy.tflite"
    }


    private fun readTestInstance(): Array<FloatArray> {
        val rows = windowsize
        val cols = 6
        val reader = BufferedReader(InputStreamReader(assets.open("test_instance_window25_0.txt")))
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

    //This is a function for storing data on the cloud.
    private fun saveData() {
        //init of database
        val db = Firebase.firestore
        val uploadTime = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.UK).format(
            Date()
        )

        val dataPacket = hashMapOf(
            "timeStamp" to uploadTime,
            "predictionList" to predictionList
        )

        db.collection(userEmailGlob)
            .add(dataPacket)
            .addOnSuccessListener {
                documentReference ->
                Toast.makeText(this,"added with ${documentReference.id}",Toast.LENGTH_SHORT)
            }
            .addOnFailureListener {
                e ->
                Toast.makeText(this,"error: ${e.message}", Toast.LENGTH_SHORT)
            }



    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        unregisterReceiver(thingyLiveUpdateReceiver)
        looperRespeck.quit()
        looperThingy.quit()
    }

}


