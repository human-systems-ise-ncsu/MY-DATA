package edu.ncsu.ise.dsl

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File

open class ActivityCommons : AppCompatActivity() {
    fun hideSystemUI() { // public func.
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    fun spreadAndFold(b1: Button, b2: LinearLayout) {
        if (b2.visibility == View.VISIBLE) {
            b1.setBackgroundResource(R.drawable.spread)
            b2.visibility = View.GONE
        } else if (b2.visibility == View.GONE) {
            b1.setBackgroundResource(R.drawable.fold)
            b2.visibility = View.VISIBLE
        }
    }

    fun howFar(courseString: String): Int {
//        return courseString
//            .toList()
//            .map { if (it.toString()=="0") 1 else 0 }
//            .joinToString("")
//            .toInt(2)
        return if (courseString=="11") 0 else if (courseString=="01") 2 else 3
    }

    fun readPc12(actionNumber: Int): List<List<Float>> {
        return File(filesDir.toString() + "/pc12_" + actionNumber + ".txt") // Int will be changed to String
            .bufferedReader()
            .use { it.readLines() }
            .map { it.split(",") }
            .map { it.map { it.toFloat() } }
    }

    fun readEigenvalues24(): List<Float> {
        return File(filesDir.toString() + "/evals24.txt")
            .bufferedReader()
            .use { it.readLines() }
            .map { it.toFloat() }
    }

    fun pc12ArrayPair(upToWhatActionNumber: Int, func: (Int) -> List<List<Float>>): Array<Pair<Float,Float>> {
        return Array(upToWhatActionNumber) { it } // Array<Int>
            .map { val pc12ListList = func(it); pc12ListList[0] zip pc12ListList[1] } // the result is in the format List<List<Pair<Float,Float>>> bc map -> List<>, zip -> List<Pair<Float,Float>>
            .flatten()
            .toTypedArray()
    }

    fun trafficLight(number: Int, buttons: List<Button>) {
        buttons.forEachIndexed { i, b ->
            when(i) {
                number -> {
                    b.setBackgroundResource(R.drawable.btn_course_on)
                    b.setTextColor(resources.getColor(R.color.wolfpack_red))
                }
                else -> {
                    b.setBackgroundResource(R.drawable.btn_course_off)
                    b.setTextColor(resources.getColor(R.color.gray_a80))
                }
            }
        }
    }

    fun pickTilDifferent(elements: Array<Pair<Float,Float>>, k: Int): Array<Pair<Float,Float>> {
//        Log.d("asd", elements.slice(0..k-1).toString())
//        Log.d("asd", elements.slice(0..k-1).distinct().count().toString())
//        Log.d("asd", arrayOf(Pair(0.36,0.43), Pair(0.36,0.43), Pair(0.36,0.43)).distinct().count().toString()) // 1
//        Log.d("asd", arrayOf(Pair(0.36,0.43), Pair(0.36,0.43), Pair(0.36,0.41)).distinct().count().toString()) // 2
//        Log.d("asd", arrayOf(Pair(0.36,0.45), Pair(0.36,0.43), Pair(0.36,0.41)).distinct().count().toString()) // 3
        do { elements.shuffle() } while (elements.slice(0..k-1).distinct().count() != k)
        return Array(k) { elements[it] }
    }

    fun doubleZeroMaker(int: Int): String {
        return "%02d".format(int)
    }
}