package dev.nauber.esphomerc

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.nauber.esphomerc.databinding.FragmentCockpitBinding
import dev.nauber.esphomerc.databinding.FragmentControllerOutputBinding

class ControllerOutputFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentControllerOutputBinding.inflate(inflater, container, false)

        val textView = binding.textView

        val viewModel: ControlCommViewModel by viewModels({requireActivity()})
        viewModel.getControllerOut().observe(viewLifecycleOwner, Observer<String>() {
            textView.text=it
        })


        val view = binding.root
        return view
    }

}