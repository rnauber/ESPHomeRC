package dev.nauber.esphomerc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dev.nauber.esphomerc.databinding.FragmentConnectionBinding

class ConnectionFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel: ControlCommViewModel by viewModels({requireActivity()})
        val binding = FragmentConnectionBinding.inflate(inflater, container, false)
        binding.floatingActionButton.setOnClickListener{
            //viewModel.reconnect(requireContext())
            viewModel.requestPing()
        }
        val view = binding.root
        return view
    }

}