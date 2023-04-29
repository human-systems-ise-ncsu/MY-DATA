package edu.ncsu.ise.dsl

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import edu.ncsu.ise.dsl.databinding.ActivityClusteringResultBinding
import space.kscience.kmath.operations.averageWith
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

class ClusteringResultActivity : ActivityCommons() {
    lateinit var binding: ActivityClusteringResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClusteringResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnClusteringResultBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        binding.btnScatterPlot.setOnClickListener { spreadAndFold(binding.btnScatterPlot, binding.llScatterPlot) } // the first item needs to show up so it has no View.GONE below
        binding.btnGroundTruth.setOnClickListener { spreadAndFold(binding.btnGroundTruth, binding.llGroundTruth) }

//        binding.llGroundTruth.visibility = View.GONE

        var k = howFar(intent.getStringExtra("course")!!) + 1 // the # of classes
        if (k==3) binding.tvClassNumber.text = "1 / 2 / 3"
        val kButtons = arrayOf(binding.btnK2, binding.btnK3, binding.btnK4)
        if (k==3) kButtons[2].visibility = View.GONE
        kButtons[k-2].setBackgroundResource(R.drawable.btn_course_on)
        kButtons[k-2].setTextColor(resources.getColor(R.color.wolfpack_red))
        binding.btnKmeansGo.text = "Choose " + k + " centroids randomly"
        val elements = pc12ArrayPair(k, ::readPc12)
        drawGraph(1, Array(1) { elements.toCollection(ArrayList()) }) // Array<Pair<Float,Float>> -> ArrayList<Pair<Float,Float>>

        var step = 1
//        var centroids = Array(k) { elements.shuffle(); elements[it] } // after .shuffle() elements are shuffled i.e., inplace=true
        var centroids = pickTilDifferent(elements, k)
        var clusters = Array(k) { ArrayList<Pair<Float,Float>>() } // arrayListOf<Pair<Float,Float>>(); val clusters = Array<ArrayList<Pair<Float,Float>>>(3) { ArrayList() } // arrayListOf()
        var kanban = true
        val pc12Means = Array(k) { readPc12(it).map { it.average().toFloat() }.zipWithNext()[0] } // zipWithNext returns a list of pairs of each two adjacent elements in this collection
        var clustersToGtClassOrder = arrayOf(0,1,2,3).slice(0..k-1)
        var clustersBefore = Array(k) { ArrayList<Pair<Float,Float>>() }
        val originalK = k
        val pc12Clusters = Array(k) { ArrayList(readPc12(it)[0] zip readPc12(it)[1]) }
        val confusionMatrixCells = mutableListOf(0,0,0,0) // tp, fp, tn, fn
        val cellStrings = Array(4) { StringBuilder() } // tp, fp, tn, fn
        val confusionMatrixCellsTvId = arrayOf(binding.tvTp, binding.tvFp, binding.tvTn, binding.tvFn)
        val confusionMatrixCellsTvOriginalTexts = arrayOf("TP\n( True Positive )", "FP\n( False Positive )", "TN\n( True Negative )", "FN\n( False Negative )")
        val performance = Array(k) { Array(4) { 0f } } // accuracy, precision, recall, f1score
        val performanceTableCellsTvId = arrayOf(binding.tvAccuracy, binding.tvPrecision, binding.tvRecall, binding.tvF1Score)
        binding.btnKmeansGo.setOnClickListener {
            vibration()
            if (step < 2) {
                drawGraph(1, Array(1) { elements.toCollection(ArrayList()) }, step++, centroids)
                binding.btnKmeansGo.setBackgroundResource(R.drawable.btn_kmeans_step1)
                binding.btnKmeansGo.text = "Assign to the clusters"
            } else {
                if (step % 2 == 0) {
                    elements.forEach { clusters[assignToCluster(it, centroids)].add(it) }
                    clustersToGtClassOrder = menMatching(centroids, pc12Means, 1) // centroids.map { assignToCluster(it, pc12Means) }
                    if (step > 2 && kanban && clustersBefore contentEquals clusters) {
                        kanban = false
                        Toast.makeText(this, "the centroids converged", Toast.LENGTH_LONG).show() // incompatible with the right above line
                    }

                    drawGraph(k, clusters, (step++).floorDiv(2), centroids, colorOrder=clustersToGtClassOrder)

                    // ##### below is for performance reporting ##################################################################################
                    if (k == originalK) {
                        arrayOf(0,1,2,3).sliceArray(0..k-1).forEach { i -> // i corresponds to the order of the gt cluster of interest
                            clustersToGtClassOrder.indices.map { clustersToGtClassOrder.indexOf(it) }.map { clusters[it] }.forEachIndexed { j, arrayList -> // clusters here corresponds to the estimated clusters (i.e., not gt clusters)
//                            Log.d("asd", pc12Clusters[i].distinct().size.toString())
//                            Log.d("asd", ArrayList(pc12Clusters.indices.filter { it != i }.map { pc12Clusters[it] }.flatten()).distinct().size.toString())
                                arrayList.forEach {
                                    when(j) {
                                        i -> if (it in pc12Clusters[i]) confusionMatrixCells[0] += 1 else confusionMatrixCells[1] += 1
                                        else -> if (it in ArrayList(pc12Clusters.indices.filter { it != i }.map { pc12Clusters[it] }.flatten())) confusionMatrixCells[2] += 1 else confusionMatrixCells[3] += 1
                                    }
                                }
                            }

                            performance[i] = arrayOf(
                                ((confusionMatrixCells[0] + confusionMatrixCells[2]).toFloat() / confusionMatrixCells.sum()), // accuracy = (tp+tn) / total
                                (confusionMatrixCells[0].toFloat() / (confusionMatrixCells[0] + confusionMatrixCells[1])), // precision = tp / (tp+fp)
                                (confusionMatrixCells[0].toFloat() / (confusionMatrixCells[0] + confusionMatrixCells[3])), // recall = tp / (tp+fn)
                                0f)
                            performance[i][3] = if (performance[i][1]+performance[i][2]==0f) 0f else (2*(performance[i][1]*performance[i][2]) / (performance[i][1]+performance[i][2])) // f1score = 2*(precision+recall) / (precision*recall)

                            arrayOf(0,1,2,3).forEach {
                                cellStrings[it].append(if (i==0) doubleZeroMaker(confusionMatrixCells[it]) else " / " + doubleZeroMaker(confusionMatrixCells[it]))
                                confusionMatrixCells.set(it, 0)
                            }
                        }
                        arrayOf(0,1,2,3).forEach {
                            confusionMatrixCellsTvId[it].text = cellStrings[it]
                            cellStrings[it].clear()
                            performanceTableCellsTvId[it].text = performance.map { array -> String.format("%.2f", array[it]) }.joinToString("\n")
                        }
//                        binding.tvPerformance.text = String.format("Precision = TP/(TP+FP) = %.2f\nRecall = TP/(TP+FN) = %.2f\n2*(Precision*Recall)/(Precision+Recall) = %.2f", performance[0])
                    }
                    // ##### above is for performance reporting ##################################################################################

                    binding.btnKmeansGo.setBackgroundResource(R.drawable.btn_kmeans_step0)
                    binding.btnKmeansGo.text = "Find new centroids"
                } else {
                    clusters.forEachIndexed { i, arrayList -> centroids[i] = findNewCentroid(arrayList) }
                    drawGraph(k, clusters, (++step).floorDiv(2), centroids, colorOrder=clustersToGtClassOrder)
                    clustersBefore = clusters.map { it.map { it.copy() } as ArrayList }.toTypedArray() // deep copy
                    clusters.forEach { it.clear() }
                    binding.btnKmeansGo.setBackgroundResource(R.drawable.btn_kmeans_step1)
                    binding.btnKmeansGo.text = "Assign to the clusters"
                }
            }
        }

        drawGraph(k, gt=true)

        kButtons.forEachIndexed { i, button ->
            button.setOnClickListener {
                vibration()
                trafficLight(i, listOf(binding.btnK2, binding.btnK3, binding.btnK4))
                confusionMatrixCellsTvId.forEachIndexed { j, textView -> textView.text = confusionMatrixCellsTvOriginalTexts[j] }
                performanceTableCellsTvId.forEachIndexed { k, textView -> textView.text = "-" }
                k = i + 2
                binding.btnKmeansGo.text = "Choose " + k + " centroids randomly"
                drawGraph(1, Array(1) { elements.toCollection(ArrayList()) })

                step = 1
                centroids = pickTilDifferent(elements, k) // before it was Array(2) { elements.shuffle(); elements[it] }
                clusters = Array(k) { ArrayList<Pair<Float,Float>>() }
                kanban = true
                clustersBefore = Array(k) { ArrayList<Pair<Float,Float>>() }
            }
        }
    }

    fun drawGraph(numClusters: Int, clusters: Array<ArrayList<Pair<Float,Float>>>? = null, iteration: Int? = null, centroids: Array<Pair<Float,Float>>? = null, gt: Boolean? = false, colorOrder: List<Int>? = listOf(0,1,2,3)) {
        val pc12Entries = Array(numClusters) { ArrayList<Entry>() }
        pc12Entries.forEachIndexed { i, arrayList ->
            if (clusters == null) {
                val pc12 = readPc12(i)
                (pc12[0] zip pc12[1]).forEach { arrayList.add(Entry(it.first, it.second)) }
            } else {
                clusters[i].forEach { arrayList.add(Entry(it.first, it.second)) }
            }
        }

        val pc12Values = pc12Entries.mapIndexed { i, arrayList -> ScatterDataSet(arrayList, if (numClusters==1) "All data" else listOf("Walking", "Standing", "Jumping jack", "Free motion")[i])}
        val pc12Colors = if (numClusters==1) arrayOf(R.color.Blue_Gray) else colorOrder!!.map { arrayOf(R.color.pc1_rose, R.color.pc2_blue, R.color.SpringGreen, R.color.amber)[it] }.toTypedArray()
//        val pc12Colors = if (numClusters==1) arrayOf(R.color.Blue_Gray) else arrayOf(R.color.pc1_rose, R.color.pc2_blue, R.color.SpringGreen, R.color.amber)
        pc12Values.forEachIndexed { i, scatterDataSet ->
            scatterDataSet.setDrawValues(false)
            scatterDataSet.color = resources.getColor(pc12Colors[i])
            scatterDataSet.setScatterShape(if (gt==true) ScatterChart.ScatterShape.CIRCLE else arrayOf(ScatterChart.ScatterShape.CIRCLE, ScatterChart.ScatterShape.TRIANGLE, ScatterChart.ScatterShape.SQUARE, ScatterChart.ScatterShape.X)[i])
            scatterDataSet.scatterShapeSize = 20f }
        val scatterChartOfInterest = if (gt==true) binding.scatterChartGroundTruth else binding.scatterChart
        scatterChartOfInterest.data = ScatterData(pc12Values)

        if (centroids != null) {
            val centroidValues = ScatterDataSet(centroids.map { Entry(it.first, it.second) }, "Centroids").apply {
                setDrawValues(false)
                color = resources.getColor(R.color.purple_500)
                setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                scatterShapeSize = 25f }
            scatterChartOfInterest.data.addDataSet(centroidValues) }

//        scatterChartOfInterest.xAxis.axisMinimum = -5f
//        scatterChartOfInterest.xAxis.axisMaximum = 5f
//        scatterChartOfInterest.xAxis.position = XAxis.XAxisPosition.BOTTOM
//        scatterChartOfInterest.axisLeft.axisMinimum = -5f
//        scatterChartOfInterest.axisLeft.axisMaximum = 5f
        scatterChartOfInterest.axisRight.isEnabled = false

        scatterChartOfInterest.setTouchEnabled(false)
//        scatterChartOfInterest.setPinchZoom(true)

        scatterChartOfInterest.description.text = iteration?.let { "Iteration $iteration" } ?: ""
        scatterChartOfInterest.description.textSize = 12f
        scatterChartOfInterest.description.textColor = resources.getColor(R.color.wolfpack_red)
//        scatterChartOfInterest.setNoDataText("No forex yet!")

        if (gt == false && numClusters >= 3) {
//            val legendEntries = Array(numClusters) { LegendEntry("Cluster " + (it+1),
//                Legend.LegendForm.LINE, // listOf(Legend.LegendForm.CIRCLE, Legend.LegendForm.SQUARE, Legend.LegendForm.LINE, Legend.LegendForm.EMPTY)[it]
//                Float.NaN, Float.NaN, null, resources.getColor(pc12Colors[it])) }
//            scatterChartOfInterest.legend.setCustom(legendEntries)
            scatterChartOfInterest.legend.isEnabled= false
        } else {
            scatterChartOfInterest.legend.form = Legend.LegendForm.CIRCLE
        }

        if (centroids == null) scatterChartOfInterest.animateY(3000)
        scatterChartOfInterest.invalidate()
    }

    fun assignToCluster(element: Pair<Float, Float>, centroids: Array<Pair<Float, Float>>): Int {
        val distances = centroids.map { kotlin.math.sqrt((it.first - element.first).pow(2) + (it.second - element.second).pow(2)) } // List<Float>
        return distances.indexOf(Collections.min(distances))
    }

    fun menMatching(centroids: Array<Pair<Float, Float>>, gtCentroids: Array<Pair<Float, Float>>, multiplier: Int): List<Int> {
        var menPreference = centroids.map { assignToCluster(it, gtCentroids) }
        if (menPreference.distinct().count() == centroids.size) {
            return menPreference
        } else {
            val men = centroids.toMutableList()
            val women = gtCentroids.toMutableList()
            val matchingResult: MutableList<Int> = ArrayList(Collections.nCopies(men.size, -1))
            val birthday = listOf(11f,28f); var party = multiplier
            women.forEachIndexed { i, woman ->
                if (i >= 1) menPreference = men.map { assignToCluster(it, women.toTypedArray()) }
//                Log.d("asd", menPreference.groupingBy { it }.eachCount().filter { it.value > 1 && it.key >= i }.keys.toList().toString())
                if (i in menPreference.groupingBy { it }.eachCount().filter { it.value > 1 && it.key >= i }.keys.toTypedArray()) {
                    Log.d("asd", menPreference.mapIndexedNotNull { j, manWant -> if (manWant == i) j else -1 }.map { if (it!=-1) men[it] else Pair(999f, 999f) }.toString())
                    val womanWant = assignToCluster(woman, menPreference.mapIndexedNotNull { j, manWant -> if (manWant == i) j else -1 }.map { if (it!=-1) men[it] else Pair(999f, 999f) }.toTypedArray())
                    matchingResult.set(womanWant, i)
                    men.set(womanWant, pairAmplifier(birthday, party))
                    women.set(i, pairAmplifier(birthday, party++))
                } else {
                    if (i in menPreference) {
                        matchingResult.set(menPreference.indexOf(i), i)
                        men.set(menPreference.indexOf(i), pairAmplifier(birthday, party))
                        women.set(i, pairAmplifier(birthday, party++))
                        return@forEachIndexed
                    }
                    return@forEachIndexed
                }
            }
            return if (-1 in matchingResult) menMatching(men.toTypedArray(),women.toTypedArray(), party) else matchingResult
        }
    }

    fun pairAmplifier(list: List<Float>, int: Int): Pair<Float,Float> {
        return list.map { it * int }.zipWithNext()[0]
    }

    fun findNewCentroid(cluster: ArrayList<Pair<Float,Float>>): Pair<Float,Float> {
        return Pair(
            String.format("%.2f", cluster.map { it.first }.average()).toFloat(), // with rounding
            String.format("%.2f", cluster.map { it.second }.average()).toFloat())
    }
}