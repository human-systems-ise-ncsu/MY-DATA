package edu.ncsu.ise.dsl

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import edu.ncsu.ise.dsl.databinding.TipFragBinding

class TipFragment : Fragment() {
    lateinit var binding : TipFragBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = TipFragBinding.inflate(inflater, container, false)

        val vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnGoCamera.setOnClickListener {
            vibrator.vibrate(VibrationEffect.createOneShot(50, 100))
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            requireActivity().supportFragmentManager.popBackStack()
        }

        return binding.root
    }


}