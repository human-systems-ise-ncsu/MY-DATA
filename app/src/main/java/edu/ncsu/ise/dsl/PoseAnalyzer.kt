package edu.ncsu.ise.dsl

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseAnalyzer(val onDetectListener: OnDetectListener): ImageAnalysis.Analyzer {

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions
            .Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // assert(imageProxy.format==ImageFormat.YUV_420_888)
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // assert(image.format==ImageFormat.YUV_420_888)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    onDetectListener.onDetect(pose)
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}