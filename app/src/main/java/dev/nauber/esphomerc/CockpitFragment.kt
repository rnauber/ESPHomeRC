package dev.nauber.esphomerc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dev.nauber.esphomerc.databinding.FragmentCockpitBinding

class CockpitFragment : Fragment() {
    val UPDATEINTERVALMS=400

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCockpitBinding.inflate(inflater, container, false)

        val imageView = binding.imageView

        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        viewModel.getImage().observe(viewLifecycleOwner, {
            imageView.setImageDrawable(it.toDrawable(resources))
        })


        val joystick = binding.joystickView
        joystick.setOnMoveListener( { angle, strength ->
            val x = (joystick.normalizedX - 50f) / 50.0f
            val y = (joystick.normalizedY - 50f) / -50.0f

            viewModel.updateInput(mapOf("x" to x, "y" to y))
        }, UPDATEINTERVALMS)

        val view = binding.root
        return view
    }

}