package edu.ncsu.ise.dsl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import edu.ncsu.ise.dsl.databinding.ActivitySecondBinding

class SecondActivity : ActivityCommons() {
    private lateinit var binding: ActivitySecondBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvName.text = "Hey " + intent.getStringExtra("name") + ","

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnSecondBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        var course = 0
        fun trafficLight(number: Int) {
            vibration()
            listOf(binding.btnPcaIntro, binding.btnClusteringIntro).forEachIndexed { i, b ->
                when (i) {
                    number -> {
                        b.setBackgroundResource(R.drawable.btn_course_on)
                        b.setTextColor(resources.getColor(R.color.wolfpack_red))
                        binding.ivChoice.setImageResource(if (number == 0) R.drawable.pca_choice else R.drawable.clustering_choice)
                    }
                    else -> {
                        b.setBackgroundResource(R.drawable.btn_course_off)
                        b.setTextColor(resources.getColor(R.color.gray_a80))
                    }
                }
            }
            course = number
        }
        binding.btnPcaIntro.setOnClickListener { trafficLight(0) }
        binding.btnClusteringIntro.setOnClickListener { trafficLight(1) }

        binding.btnGoNext.setOnClickListener {
            vibration()
            val intent = Intent(this, if (course == 0) PcaIntroActivity::class.java else ClusteringIntroActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}