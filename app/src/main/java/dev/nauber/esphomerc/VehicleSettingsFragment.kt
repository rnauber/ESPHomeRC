package dev.nauber.esphomerc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import dev.nauber.esphomerc.databinding.FragmentControllerBinding
import dev.nauber.esphomerc.databinding.FragmentVehicleSettingsBinding

class VehicleSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })
        val binding = FragmentVehicleSettingsBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel
        return binding.root
    }

}