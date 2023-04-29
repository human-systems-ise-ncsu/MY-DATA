package edu.ncsu.ise.dsl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import edu.ncsu.ise.dsl.databinding.ActivityClusteringIntroBinding

class ClusteringIntroActivity : ActivityCommons() {
    lateinit var binding: ActivityClusteringIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClusteringIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnClusteringIntroBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        binding.btnClusteringIntroNext.setOnClickListener {
            vibration()
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("course", "1")
            startActivity(intent)
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}