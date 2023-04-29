package edu.ncsu.ise.dsl

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import edu.ncsu.ise.dsl.databinding.ActivityPcaResultBinding
import java.io.File
import android.util.Log
import com.github.mikephil.charting.data.*
import java.math.RoundingMode
import java.text.DecimalFormat

class PcaResultActivity : ActivityCommons() {
    private lateinit var binding: ActivityPcaResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPcaResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnPcaResultBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        binding.btnHowToGetPcs.setOnClickListener { spreadAndFold(binding.btnHowToGetPcs, binding.llHowToGetPcs) } // the first item needs to show up so it has no View.GONE below
        binding.btnScreePlot.setOnClickListener { spreadAndFold(binding.btnScreePlot, binding.llScreePlot) }
        binding.btnPcTimeSeries.setOnClickListener { spreadAndFold(binding.btnPcTimeSeries, binding.llPcTimeSeries) }

//        binding.llScreePlot.visibility = View.GONE
//        binding.llPcTimeSeries.visibility = View.GONE

//        #####below is the scree plot####################################
        val evals24 = readEigenvalues24()

        val evalsEntries = ArrayList<BarEntry>()
        (1..24).forEach { evalsEntries.add(BarEntry(it.toFloat(), evals24[it-1])) } // listOf(1, 2, ..., last value), first "it" is the floats of the evals24, and second "it" is each of (1..24)

        val evalsDataSet = BarDataSet(evalsEntries, "")
//        evalsDataSet.setDrawValues(false)
        evalsDataSet.color = resources.getColor(R.color.purple_200)
        evalsDataSet.valueTextColor = resources.getColor(R.color.purple_200)
//        evalsDataSet.valueTextSize = 10f
//        evalsDataSet.setGradientColor(resources.getColor(R.color.pc2_blue), resources.getColor(R.color.pc1_rose)) // can set color like this: Color.parseColor("#00FF5722"),Color.parseColor("#FFFF5722")

        binding.barChart.data = BarData(evalsDataSet)

//        binding.barChart.data.barWidth = 0.8f

//        binding.barChart.xAxis.axisMinimum = -5f
//        binding.barChart.xAxis.axisMaximum = 32f
//        binding.barChart.axisLeft.axisMinimum = -3f
//        binding.barChart.axisLeft.axisMaximum = 3f
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.axisLeft.isEnabled = false
        binding.barChart.xAxis.isEnabled = false
//        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        binding.barChart.setTouchEnabled(true) // default is true
        binding.barChart.setPinchZoom(false) // if above is false, it is ignored; default is true

        binding.barChart.description.text = ""
//        binding.barChart.setNoDataText("No forex yet!")

        binding.barChart.legend.setCustom(listOf(LegendEntry("Eigenvalues { λ_k : k=1,2,...,24 }", Legend.LegendForm.CIRCLE, Float.NaN, Float.NaN,null, resources.getColor(R.color.purple_200))))

        binding.barChart.animateX(1300)

        binding.barChart.marker = CustomMarkerScreePlot(this@PcaResultActivity, R.layout.marker_view, evals24)
//        #####above is the the scree plot #################################

//        #####below is the pc time series line graph
        val pc12 = readPc12(0) // readPc12(0) -- opt1 // readPc12(0)[0] + readPc12(0)[0] -- opt2
        val pc12Entries = listOf(ArrayList<Entry>(), ArrayList<Entry>())
        val pc12Interval = 1 // 1 for opt1 2 for opt2
        pc12Entries.forEachIndexed { i, arrayList -> // i starts w/ 0
            pc12[i].forEachIndexed { j, fl -> // j starts with 0, for opt2 remove [i] in front
                if(j % pc12Interval == 0) {arrayList.add(Entry((j.toFloat() / pc12Interval) + 1, fl))} // opt1

//                if(i == 1) { // make pc2 have the interval of one data point (if pc12Interval = 1) // opt2
//                    if(j % pc12Interval == 0) {arrayList.add(Entry((j.toFloat() / pc12Interval) + 1, fl))}
//                } else {
//                    if(j + 1 <= (pc12).size / pc12Interval) {arrayList.add(Entry(j.toFloat() + 1, fl))}
//                }

            }
        }

        val pc12Values = pc12Entries.map { LineDataSet(it, "") }
        val pc12Colors = listOf(R.color.pc1_rose, R.color.pc2_blue)
        pc12Values.forEachIndexed { index, lineDataSet ->
            lineDataSet.setDrawValues(false)

            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER)
            lineDataSet.setCubicIntensity(0.23f)

            lineDataSet.color = resources.getColor(pc12Colors[index])
            lineDataSet.lineWidth = 3f

            lineDataSet.setCircleColor(resources.getColor(pc12Colors[index]))
            lineDataSet.circleRadius = 4f

            lineDataSet.setDrawCircleHole(false)
//            lineDataSet.circleHoleRadius = 4f
//            lineDataSet.circleHoleColor = ContextCompat.getColor(this, pc12Colors[index])

            lineDataSet.setDrawFilled(true)
            lineDataSet.fillColor = resources.getColor(pc12Colors[index])
            lineDataSet.fillAlpha = 50 // max 255
        }

//        binding.lineChart.xAxis.labelRotationAngle = 0f

        binding.lineChart.data = LineData(pc12Values)

        binding.lineChart.xAxis.axisMinimum = 0f
        binding.lineChart.xAxis.axisMaximum = 100f // pc12[0].size.toFloat() / pc12Interval
//        binding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
//        binding.lineChart.axisLeft.axisMinimum = -3f
//        binding.lineChart.axisLeft.axisMaximum = 3f
        binding.lineChart.axisRight.isEnabled = false

        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.setPinchZoom(true)

        binding.lineChart.description.text = ""
//        binding.lineChart.setNoDataText("No forex yet!")

        val legendEntries = listOf(
            LegendEntry("PC 1", Legend.LegendForm.LINE, Float.NaN, Float.NaN,null, resources.getColor(R.color.pc1_rose)),
            LegendEntry("PC 2", Legend.LegendForm.LINE, Float.NaN, Float.NaN,null, resources.getColor(R.color.pc2_blue)))
        binding.lineChart.legend.setCustom(legendEntries)

        binding.lineChart.animateX(5000) // Easing.EaseInExpo

        binding.lineChart.marker = CustomMarkerPcTimeSeries(this@PcaResultActivity, R.layout.marker_view)
//        #####above is the pc time series line graph
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}