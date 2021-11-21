package com.specknet.pdiotapp.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.data_item.view.*
import java.util.*
import kotlin.collections.ArrayList

class ItemAdapter(val context: Context, val items: ArrayList<Map<String,Object>>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    /**
     * Inflates the item views which is designed in xml layout file
     *
     * create a new
     * {@link ViewHolder} and initializes some private fields to be used by RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.data_item,
                parent,
                false
            )
        )
    }

    /**
     * Binds each item in the ArrayList to a view
     *
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items.get(position)

        Log.d("ItemAdapter", "Got to onBindViewHolder")
        //We need to init our piechart here.
        var months = arrayOf (
            "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
                )
        //Format our timeStamp here
        //Import format is dd-mm-yyyy_hh-mm-ss
        val ts = data.get("timeStamp") as String
        val tsSplit = ts.split('-')
        val day = tsSplit[0]
        var dayApp = "th"
        if (day.equals("21") or day.equals("1") or day.equals("31")){
            dayApp = "st"
        } else if (day.equals("22") or day.equals("2") or day.equals("32")) {
            dayApp = "nd"
        } else if (day.equals("23") or day.equals("3") or day.equals("33")) {
            dayApp = "rd"
        }

        val month = months[tsSplit[1].toInt()-1]



        val year = tsSplit[2].split("_")[0]
        val txt = "${day}${dayApp} ${month} ${year}"



        holder.tvTimeStamp.text = txt

        //Making piechart here

        //pieChartSetUpHere
        holder.pieChart.setDrawHoleEnabled(true)
        holder.pieChart.setUsePercentValues(false)
        holder.pieChart.setDrawEntryLabels(false)
        //holder.pieChart.setEntryLabelTextSize(12f)
        //holder.pieChart.setEntryLabelColor(Color.BLACK)
        holder.pieChart.description = null
        holder.pieChart.centerText = "Activities Done"

        var l : Legend = holder.pieChart.legend
        l.isEnabled = false
        //l.verticalAlignment = (Legend.LegendVerticalAlignment.TOP)
        //l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        //l.orientation = Legend.LegendOrientation.VERTICAL
        //l.setDrawInside(false)
        //l.setEnabled(true)



        val pl = data.get("predictionList") as ArrayList<Long>
        var labels = arrayOf("Sitting",
        "Sitting bent forward", "Sitting bent backward",
            "Standing",
            "Lying down on back", "Lying down left",
            "Lying down right", "Lying down on stomach",
            "Walking at normal speed",
            "Running",
            "Climbing stairs", "Descending stairs",
            "Movement", "Desk work",
            "Falling on knees", "Falling on the left",
            "Falling on the right", "Falling on the back")

        var dataCulmination : MutableMap<Int, Int> = mutableMapOf(
            0 to 1, 1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0, 7 to 0,
            8 to 0, 9 to 0, 10 to 0, 11 to 0, 12 to 0, 13 to 0, 14 to 0,
            15 to 0, 16 to 0, 17 to 0,
        )

        for (pred in pl){
            dataCulmination[pred.toInt()] = 1 + dataCulmination[pred.toInt()]!!
        }

        //Setting colours
        var colors : ArrayList<Int> = ArrayList()

        for (i in ColorTemplate.MATERIAL_COLORS ){
            colors.add(i as Int)
        }

        for (i in ColorTemplate.VORDIPLOM_COLORS ){
            colors.add(i as Int)
        }


        Log.d("ItemAdapter", "colors: ${colors}")

        //Setting entries
        var entries:ArrayList<PieEntry> = ArrayList()
        for (i in 0..17){
            if (dataCulmination[i]!! > 0){
                entries.add(PieEntry(dataCulmination[i]!!.toFloat(),labels[i]))
            }
        }

        var dataSet : PieDataSet = PieDataSet(entries, "Activity")
        dataSet.setColors(colors)
        var pieData : PieData = PieData(dataSet)
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(holder.pieChart))
        pieData.setValueTextSize(0f)
        pieData.setValueTextColor(Color.BLACK)

        holder.pieChart.setData(pieData)
        holder.pieChart.invalidate()


        //setting up to new activity button here
        //This button will move us to the display view passing through the piechart details
        holder.toActivityBtn.setOnClickListener {
            val intent =
                Intent(context,DataDisplayActivity::class.java)
            intent.putExtra("formattedTime", txt)
            intent.putExtra("pl", pl)
            Log.d("ItemAdapter", "Passing the pl: ${pl}")
            context.startActivity(intent)
        }

    }

    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each item to
        val tvTimeStamp = view.tv_timestamp
        val cardViewItem = view.card_view
        val pieChart = view.pie_chart
        val toActivityBtn = view.btn_to_display_view
    }
}