package dev.nauber.esphomerc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dev.nauber.esphomerc.databinding.FragmentControllerOutputBinding

class ControllerOutputFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentControllerOutputBinding.inflate(inflater, container, false)

        val textView = binding.textView

        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        viewModel.getControllerOut().observe(viewLifecycleOwner, {
            textView.text = it
        })

        val view = binding.root
        return view
    }


}