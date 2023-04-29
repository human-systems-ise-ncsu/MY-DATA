package edu.ncsu.ise.dsl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import edu.ncsu.ise.dsl.databinding.ActivityTableBinding
import space.kscience.kmath.nd.get
import space.kscience.kmath.nd.mapToMutableBuffer
import space.kscience.kmath.tensors.core.*
import java.io.File

class TableActivity : ActivityCommons() {
    private lateinit var binding: ActivityTableBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnTableBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        val course = intent.getStringExtra("photodog")!!
        binding.btnTableNext.setOnClickListener {
            vibration()
            val intent = Intent(this, if (course[0].toString()=="1") PcaResultActivity::class.java else ClusteringResultActivity::class.java)
            intent.putExtra("course", course)
            startActivity(intent)
        }

        when(course) {
            "11" -> binding.llActionButtons.visibility = View.GONE // 11 means pca
            "01" -> binding.btnD.visibility = View.GONE // 01 means w/o free motion, 00 means w/ free motion
        }

        val cellList = listOf(
            binding.tvR1c1, binding.tvR1c2, binding.tvR1c3, binding.tvR1c4, binding.tvR1c5, binding.tvR1c6, binding.tvR1c7, binding.tvR1c8, binding.tvR1c9, binding.tvR1c10, binding.tvR1c11, binding.tvR1c12,
            binding.tvR2c1, binding.tvR2c2, binding.tvR2c3, binding.tvR2c4, binding.tvR2c5, binding.tvR2c6, binding.tvR2c7, binding.tvR2c8, binding.tvR2c9, binding.tvR2c10, binding.tvR2c11, binding.tvR2c12,
            binding.tvR3c1, binding.tvR3c2, binding.tvR3c3, binding.tvR3c4, binding.tvR3c5, binding.tvR3c6, binding.tvR3c7, binding.tvR3c8, binding.tvR3c9, binding.tvR3c10, binding.tvR3c11, binding.tvR3c12,
            binding.tvR4c1, binding.tvR4c2, binding.tvR4c3, binding.tvR4c4, binding.tvR4c5, binding.tvR4c6, binding.tvR4c7, binding.tvR4c8, binding.tvR4c9, binding.tvR4c10, binding.tvR4c11, binding.tvR4c12,
            binding.tvR5c1, binding.tvR5c2, binding.tvR5c3, binding.tvR5c4, binding.tvR5c5, binding.tvR5c6, binding.tvR5c7, binding.tvR5c8, binding.tvR5c9, binding.tvR5c10, binding.tvR5c11, binding.tvR5c12
        )
        fun tableUpdate(poseData: List<String>) {
            val poseDataPair = poseData
                .chunked(2)
                .map { it[0] } // .joinToString(", ")
            cellList.forEachIndexed { i, tv -> tv.text = poseDataPair[i] }
        }

        fun readPoseBowl(number: Int): List<String> {
            return File(String.format("%s/%d.txt", filesDir.toString(), number))
                .bufferedReader()
                .use { it.readLine().split(",") }
        }
        tableUpdate(readPoseBowl(0))

        binding.btnCollectedData.setOnClickListener { spreadAndFold(binding.btnCollectedData, binding.llCollectedData) }
        binding.llKeyPoints.visibility = View.GONE
        binding.btnKeyPoints.setOnClickListener { spreadAndFold(binding.btnKeyPoints, binding.llKeyPoints) }

        val buttons = listOf(binding.btnW, binding.btnE, binding.btnP, binding.btnD)
        binding.btnW.setOnClickListener { vibration(); trafficLight(0, buttons); readPoseBowl(0).let { tableUpdate(it) } }
        binding.btnE.setOnClickListener { vibration(); trafficLight(1, buttons); readPoseBowl(1).let { tableUpdate(it) } }
        binding.btnP.setOnClickListener { vibration(); trafficLight(2, buttons); readPoseBowl(2).let { tableUpdate(it) } }
        binding.btnD.setOnClickListener { vibration(); trafficLight(3, buttons); readPoseBowl(3).let { tableUpdate(it) } }

        listOf(0,1,2,3).slice(0..howFar(course)).forEach { pca(readPoseBowl(it), it.toString(), course[0].toString()) }
    }

    private fun pca(poseData: List<String>, number: String, module: String) = Double.tensorAlgebra.withBroadcast {  // work in context with broadcast methods
        val poseList = mutableListOf<DoubleTensor>()
        poseData
            .map { it.toDouble() } // List<Double>
            .chunked(24) // List<List<Double>>
            .map { it.toDoubleArray() } // List<DoubleArray>
            .forEach { poseList.add(fromArray(intArrayOf(24), it)) }
        val dataset = stack(poseList).transpose()

        val preprocessedPoseList = mutableListOf<DoubleTensor>()
        for (i in 0..23) {
            val mean = dataset[i].mean()
            var std = dataset[i].std()
            if (std == 0.0) std = 1.0

            val min = dataset[i].min()
            val max = dataset[i].max()

            val preprocessedData = if (module=="1") (dataset[i]-mean) / std else (dataset[i]-min)/(max-min) // module=="1" means pca module, 0 means clustering module
//            Log.d("asd",module.toString())
            preprocessedPoseList.add(preprocessedData) // min-max scaling: (dataset[i]-min)/(max-min), standardization: (dataset[i] - mean) / std
        }

        val standardizedPoseDoubleTensor = stack(preprocessedPoseList)
        val (evals, evecs) = cov(preprocessedPoseList).symEig()

        val evalsList = (0..23).map { evals[it].get().toFloat() }
        val evalsIndicesInDescendingOrder = evalsList.indices.sortedByDescending { evalsList[it] } // sorted based on elements in {} like evalsList[0] associated with the index 0, evalsList[1] associated with the index 1, ...
//        val evalsIndicesInDescendingOrder2 = arrayOf(evalsList.indexOf(evalsList.sortedDescending()[0]), evalsList.indexOf(evalsList.sortedDescending()[1])) // can do like this but look messy
        val evals24InDescendingOrder = (0..23).map { String.format("%.4f", evalsList[evalsIndicesInDescendingOrder[it]]) }
        openFileOutput("evals24.txt", MODE_PRIVATE)
            .use { it.write(evals24InDescendingOrder.joinToString("\n").toByteArray()) }

        val pc12 = listOf(evecs[evalsIndicesInDescendingOrder[0]] dot standardizedPoseDoubleTensor, evecs[evalsIndicesInDescendingOrder[1]] dot standardizedPoseDoubleTensor)
            .map { it.copyArray().joinToString(separator = ",") { String.format("%.4f", it) } } // change to %.4f from %.2f to remove the same pc12 data in the clustering module
        openFileOutput("pc12_" + number + ".txt", MODE_PRIVATE)
            .use { it.write(pc12.joinToString("\n").toByteArray()) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}