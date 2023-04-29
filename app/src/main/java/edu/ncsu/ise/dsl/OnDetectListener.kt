package edu.ncsu.ise.dsl

import com.google.mlkit.vision.pose.Pose

interface OnDetectListener {
    fun onDetect(pose: Pose)
}