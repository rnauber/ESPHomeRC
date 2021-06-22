package dev.nauber.esphomerc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import dev.nauber.esphomerc.databinding.FragmentCockpitBinding
import kotlin.math.cos
import kotlin.math.sin

class CockpitFragment : Fragment() {
    private val UPDATEINTERVALMS = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCockpitBinding.inflate(inflater, container, false)

        val imageView = binding.imageView
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        viewModel.liveImage.observe(viewLifecycleOwner, {
            imageView.setImageDrawable(it.toDrawable(resources))
        })
        viewModel.liveCamRotation.observe(viewLifecycleOwner,{
            imageView.rotation = it
        })

        val joystick = binding.joystickView
        joystick.setOnMoveListener({ angle, strength ->
            val rad = angle / 180f * 3.141f
            val x = cos(rad) * strength / 100f
            val y = sin(rad) * strength / 100f

            viewModel.updateInput(mapOf("x" to x, "y" to y))
        }, UPDATEINTERVALMS)

        val view = binding.root
        return view
    }

}