package edu.ncsu.ise.dsl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import edu.ncsu.ise.dsl.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class CameraActivity : ActivityCommons() {
    private lateinit var binding: ActivityCameraBinding
    private val PERMISSIONS_REQUIRD = arrayOf(Manifest.permission.CAMERA)
    private val PERMISSIONS_REQEST_CODE = 1
    private var recording = false
    private val recordingTime = 10 // needs to be 10 but for development I set it to small #
    private val notFinishedActions = mutableListOf(0,1,2,3)
    private lateinit var actionButtons: List<Button>
    private lateinit var course: String
    private var canGoToNext = 0
    override fun onCreate(savedInstanceState: Bundle?) { // this is called earlier than onResume
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFrag()

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnCameraBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        course = intent.getStringExtra("course")!!
        binding.tvNext.visibility = View.INVISIBLE
        binding.btnCameraNext.setOnClickListener {
            if (canGoToNext==0) {
                if (course=="1") { Toast.makeText(this, "please recording at least 3 motions: walking, standing, jumping jack", Toast.LENGTH_LONG).show() }
                else { Toast.makeText(this, "please recording a walking motion", Toast.LENGTH_SHORT).show() }
            } else {
                val intent = Intent(this, TableActivity::class.java)
                intent.putExtra("photodog", listOf(2,3).map {if (it in notFinishedActions) 1 else 0}.joinToString("")) // 11-pca, 01-clustering w/o free motion, 00-clustering w/ free motion
                startActivity(intent)
            }
        }

        actionButtons = listOf(binding.btnWalking, binding.btnEating, binding.btnPhoto, binding.btnDog)
        if (course=="0") {
            actionButtons.forEach { it.visibility = View.GONE }
        } else {
            fun trafficLight(number: Int) {
                actionNumber = number
                actionButtons.forEachIndexed { i, b ->
                    when(i) {
                        number -> {
                            if (i in notFinishedActions) {
                                b.setBackgroundResource(R.drawable.btn_action_on)
                                b.setTextColor(resources.getColor(R.color.wolfpack_red))
                            } else {
                                b.setBackgroundResource(R.drawable.btn_recapture)
                                b.setTextColor(resources.getColor(R.color.white))
                            }
                        }
                        else -> {
                            if (i in notFinishedActions) {
                                b.setBackgroundResource(R.drawable.btn_action_off)
                                b.setTextColor(resources.getColor(R.color.white))
                            } else {
                                b.setBackgroundResource(R.drawable.btn_capture_finished)
                                b.setTextColor(resources.getColor(R.color.white))
                            }
                        }
                    }
                }
            }
            binding.btnWalking.setOnClickListener { trafficLight(0) }
            binding.btnEating.setOnClickListener { trafficLight(1) }
            binding.btnPhoto.setOnClickListener { trafficLight(2) }
            binding.btnDog.setOnClickListener { trafficLight(3) }
        }

        binding.btnRecord.setOnClickListener {
            vibration()
            recording = true
            binding.btnRecord.visibility = View.INVISIBLE
            binding.tvRecord.visibility = View.INVISIBLE
            binding.tvCountdown.visibility = View.VISIBLE
            thread (start=true) {
                for(i in recordingTime downTo 0) {
                    runOnUiThread { binding.tvCountdown.text = "$i" }
                    Thread.sleep(1000) // 1000=1s
                }
            }
        }

        if (!hasPermissions(this)){
            requestPermissions(PERMISSIONS_REQUIRD, PERMISSIONS_REQEST_CODE)
        } else {
            startCamera()
        }
    }

    private fun setFrag() {
        val ft = supportFragmentManager.beginTransaction() // fragment transaction
        ft.replace(R.id.fl_tips, TipFragment()).commit()
    }

    override fun onResume() {
        super.onResume()
        binding.btnRecord.visibility = View.VISIBLE
        binding.tvRecord.visibility = View.VISIBLE
        binding.tvCountdown.visibility = View.INVISIBLE
        binding.tvCountdown.text = recordingTime.toString()
    }

    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRD.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                Toast.makeText(this, "permission given", Toast.LENGTH_SHORT).show()
                startCamera()
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider> // ListenableFuture sets actions when a task is done correctly; C.f., Future is used to check if a task is done correctly in parallel programming
    fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable { // run when the task of the cameraProviderFuture
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = getPreview()
            val imageAnalysis = getImageAnalysis()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    fun getPreview(): Preview { // return preview object
        val preview: Preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.pvCamera.surfaceProvider)

        return preview
    }

    private val poseBowl = mutableListOf<Int>()
    private val landmarkPairs = listOf(0, 3, 0, 1, 0, 3, 4, 6, 7, 6, 9, 10) zip listOf(6, 9, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11)
    private val sampleSize = 100
    private var actionNumber = 0
    private var isOpen = 1
    private var frameOrder_ = 0
    private val frameIntervalToTake = 1
    fun getImageAnalysis(): ImageAnalysis {
        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder().build()

        imageAnalysis.setAnalyzer(cameraExecutor,
            PoseAnalyzer(object: OnDetectListener {
                override fun onDetect(pose: Pose) {
                    val landMarks = arrayOf(
                        pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER), pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
                        pose.getPoseLandmark(PoseLandmark.LEFT_WRIST), pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
                        pose.getPoseLandmark(PoseLandmark.LEFT_KNEE), pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE),
                        pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER), pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
                        pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST), pose.getPoseLandmark(PoseLandmark.RIGHT_HIP),
                        pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE), pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
                    )

                    binding.skeleton.clear()

                    landmarkPairs.forEach {
                        if (landMarks[it.first] != null && landMarks[it.second] != null) binding.skeleton.drawLine(landMarks[it.first], landMarks[it.second])
                    }

                    for (landMark in landMarks) {
                        if(landMark != null) binding.skeleton.drawCircle(landMark)
                    }

                    if (recording == true) {
                        if (poseBowl.size < 12*2*sampleSize) {
                            if(frameOrder_++ % frameIntervalToTake == 0) {
                                for (landMark in landMarks) {
                                    if(landMark != null) {
                                        poseBowl.addAll(listOf(landMark.position.x.toInt(), landMark.position.y.toInt()))
                                    } else {
                                        poseBowl.addAll(listOf(-1, -1))
                                    }
                                }
                            }
                        }

                        if (poseBowl.size == 12*2*sampleSize && binding.tvCountdown.text == "0") {
                            recording = false

                            openFileOutput(actionNumber.toString() + ".txt", MODE_PRIVATE).use {
                                it.write(poseBowl.joinToString(",").toByteArray())
                            }

                            poseBowl.clear()

                            Toast.makeText(this@CameraActivity, String.format("%s saved", listOf("\"walking\"", "\"standing\"", "\"jumping jack\"", "\"free motion\"")[actionNumber]), Toast.LENGTH_SHORT).show()

                            if (course == "1") {
                                notFinishedActions.remove(actionNumber)
                                if (notFinishedActions.size > 0) {
                                    actionButtons[notFinishedActions.first()].performClick()
                                } else {
                                    actionButtons[actionNumber].performClick()
                                }
                            }

                            onResume()

                            if (isOpen == 1 && (course == "0" || listOf(0,1,2).map { it !in notFinishedActions }.all { it } )) {
                                Toast.makeText(this@CameraActivity, "you may try again as many times as you'd like to get the best data", Toast.LENGTH_LONG).show() // note @CameraActivity
                                binding.btnCameraNext.alpha = 1.0f
                                binding.tvNext.visibility = View.VISIBLE
                                canGoToNext = 1
                                isOpen -= 1
                            }
                        }
                    }
                }
            }))
        return imageAnalysis
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}