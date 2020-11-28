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
    val UPDATEINTERVALMS = 400

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCockpitBinding.inflate(inflater, container, false)


        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val rot = sharedPreferences.getString("cam_rotation", "0")

        val imageView = binding.imageView
        imageView.rotation = rot?.toFloat() ?: 0f

        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        viewModel.getImage().observe(viewLifecycleOwner, {
            imageView.setImageDrawable(it.toDrawable(resources))
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