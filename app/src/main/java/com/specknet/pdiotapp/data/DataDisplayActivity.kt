package com.specknet.pdiotapp.data

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.activity_data_display.*
import kotlinx.android.synthetic.main.activity_data_display.view.*


class DataDisplayActivity : AppCompatActivity() {

    lateinit var textView: TextView
    lateinit var pieChart: PieChart
    lateinit var pl : ArrayList<Long>
    lateinit var txt : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_display)
        textView = date_view
        pieChart = pie_chart
        val bundle : Bundle = intent.extras!!
        val recv = bundle.get("pl")
        Log.d("DataDisplayActivity", "receiving pl ${recv}")
        txt = intent.getStringExtra("formattedTime").toString()
        pl = recv as ArrayList<Long>

        //Set textView info here
        textView.text = txt

        //pieChartSetUpHere
        pieChart.setDrawHoleEnabled(true)
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelTextSize(16f)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.description = null
        pieChart.centerText = "Activities Done"
        pieChart.setCenterTextSize(20f)

        var l : Legend = pieChart.legend
        l.isEnabled = false
        l.verticalAlignment = (Legend.LegendVerticalAlignment.TOP)
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(false)
        l.setEnabled(true)

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
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(0f)
        pieData.setValueTextColor(Color.BLACK)

        pieChart.setData(pieData)
        pieChart.invalidate()



        //Set pieChart info here

    }
}