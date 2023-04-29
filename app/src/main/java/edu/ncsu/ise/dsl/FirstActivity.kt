package edu.ncsu.ise.dsl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import edu.ncsu.ise.dsl.databinding.ActivityFirstBinding

class FirstActivity : ActivityCommons() {
    private lateinit var binding: ActivityFirstBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        fun vibration() = vibrator.vibrate(VibrationEffect.createOneShot(50, 100)) // amplitude, 0 ~ 255
        binding.btnGoSecond.setOnClickListener {
            vibration()
//            if (binding.etName.text.toString() != "") {
//                val intent = Intent(this, SecondActivity::class.java)
//                intent.putExtra("name", binding.etName.text.toString())
//                startActivity(intent)
//            } else {
//                Toast.makeText(this, "enter your name", Toast.LENGTH_SHORT).show()
//            }
            val intent = Intent(this, SecondActivity::class.java)
            intent.putExtra("name", binding.etName.text.toString())
            startActivity(intent)
        }
    }

    private var lastTimeBackPressed: Long = 0
    override fun onBackPressed() {
        if (System.currentTimeMillis() - lastTimeBackPressed > 3000) {
            Toast.makeText(this, "the app will end, if you press back button again", Toast.LENGTH_SHORT).show()
            lastTimeBackPressed = System.currentTimeMillis()
        } else {
            finish() // there's no need for finishAffinity() bc there's no activity before this (remember SplashActivity was finished)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }
}