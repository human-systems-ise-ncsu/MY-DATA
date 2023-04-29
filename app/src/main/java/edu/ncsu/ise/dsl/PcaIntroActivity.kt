package edu.ncsu.ise.dsl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import edu.ncsu.ise.dsl.databinding.ActivityPcaIntroBinding

class PcaIntroActivity : ActivityCommons() {
    lateinit var binding: ActivityPcaIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPcaIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
        binding.btnPcaIntroBack.setOnClickListener {
            vibration()
            onBackPressed()
        }

        Glide.with(this).load(R.raw.pca_gif).into(binding.ivPcaGif)

        binding.btnPca.setOnClickListener { spreadAndFold(binding.btnPca, binding.llPca) }
        binding.btnWhyPca.setOnClickListener { spreadAndFold(binding.btnWhyPca, binding.llWhyPca) }
        binding.btnPrerequisiteKnowledge.setOnClickListener { spreadAndFold(binding.btnPrerequisiteKnowledge, binding.llPrerequisiteKnowledge) }

        binding.llWhyPca.visibility = View.GONE
        binding.llPrerequisiteKnowledge.visibility = View.GONE


        binding.btnPcaIntroNext.setOnClickListener {
            vibration()
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("course", "0")
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}