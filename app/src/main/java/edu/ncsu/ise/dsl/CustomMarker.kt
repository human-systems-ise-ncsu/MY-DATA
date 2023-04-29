package edu.ncsu.ise.dsl

import android.content.Context
import android.view.LayoutInflater
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import edu.ncsu.ise.dsl.databinding.MarkerViewBinding

class CustomMarkerPcTimeSeries(context: Context, layoutResource: Int):  MarkerView(context, layoutResource) {
    private lateinit var binding: MarkerViewBinding

    init { // inflate binding and add as view
        binding = MarkerViewBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
    }

    override fun refreshContent(entry: Entry?, highlight: Highlight?) {
        val value = entry?.y?.toDouble() ?: 0.0
        var resText = ""
        if (value.toString().length > 8) {
            resText = "Val: " + value.toString().substring(0, 7)
        } else {
            resText = "Val: " + value.toString()
        }
        binding.tvValue.text = resText
        super.refreshContent(entry, highlight)
    }

    override fun getOffsetForDrawingAtPoint(xpos: Float, ypos: Float): MPPointF { // this is different from the same name fun in the CustomMarkerScreePlot class
        return MPPointF(-width/2f, -height-10f)
    }
}

class CustomMarkerScreePlot(context: Context, layoutResource: Int, private val dataToDisplay: List<Float>) : MarkerView(context, layoutResource) {
    private lateinit var binding: MarkerViewBinding

    init {
        binding = MarkerViewBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val xAxis = e?.x?.toInt() ?: 0 // (x: 1f), (x: 2f), ...
        binding.tvValue.text = String.format("λ_%d\n= %.4f\n(%.2f%%)", xAxis, dataToDisplay[xAxis-1], (dataToDisplay[xAxis-1]/dataToDisplay.sum())*100 ) // 1(=xAxis)-1 = 0 is connected to (y: λ_1), ...
        super.refreshContent(e, highlight)
    }

    override fun getOffsetForDrawingAtPoint(xpos: Float, ypos: Float): MPPointF {
        if (xpos==59.15104f) { return MPPointF((-width/2f)+102f, -height+37f) } // x - go left, y - go up; to find xpos, in the debugging mode, touch the bar of interest
        else return MPPointF(-width/2f, -height-10f)

    }
//    override fun getOffset(): MPPointF { // above can be replaced with this
//        return MPPointF(-(width / 2f), -height.toFloat())
//    }
}