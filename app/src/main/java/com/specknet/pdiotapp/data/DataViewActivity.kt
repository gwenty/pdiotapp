package com.specknet.pdiotapp.data

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.activity_data_view.*

class DataViewActivity : AppCompatActivity() {

    lateinit var recyclerView : RecyclerView
    lateinit var itemAdapter: ItemAdapter
    lateinit var email : String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_view)

        recycler_view.layoutManager = LinearLayoutManager(this)
        getDataFromFirebase()
        val itemAdapter = ItemAdapter(this, getDataFromFirebase())
        //Log.d("DataView",items.get(0) as String)

        recycler_view.adapter = itemAdapter
    }

    private fun getDataFromFirebase(): ArrayList<Map<String, Object>> {
        val db = Firebase.firestore
        var list_of_docs : ArrayList<Map<String, Object>> = ArrayList()

        email = intent.getStringExtra("email_id")!!

        //TODO: change this to accept whatever email is passed through
        db.collection(email)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var myData: Map<String, Object>  = document.getData() as Map<String, Object>
                    list_of_docs.add(myData)
                    Log.d("DataView","${myData}")
                }
                itemAdapter = ItemAdapter(this, list_of_docs)
                //Log.d("DataView",items.get(0) as String)

                recycler_view.adapter = itemAdapter
            }
            .addOnFailureListener { exception ->

            }

        Log.d("DataView",list_of_docs.size.toString())
        return list_of_docs

    }

    interface DataCallBack {
        fun onCallback(value: Map<String, Object>)
    }

}