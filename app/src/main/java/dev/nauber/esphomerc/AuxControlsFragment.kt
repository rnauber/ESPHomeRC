package dev.nauber.esphomerc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.slider.Slider
import dev.nauber.esphomerc.databinding.FragmentAuxControlsBinding

class AuxControlsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentAuxControlsBinding.inflate(inflater, container, false)
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        binding.aux0.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
                viewModel.updateInput(mapOf("aux0" to slider.value))
            }
        })
//        binding.aux0.addOnChangeListener { slider, value, fromUser ->
//                viewModel.updateInput(mapOf("aux0" to value))
//        }
        val view = binding.root
        return view
    }

}