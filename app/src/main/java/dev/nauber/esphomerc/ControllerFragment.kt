package dev.nauber.esphomerc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import dev.nauber.esphomerc.databinding.FragmentControllerBinding

class ControllerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentControllerBinding.inflate(inflater, container, false)
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })


        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val controllerSrc = sharedPreferences.getString("controller_src", Controller.DEFAULTSCRIPT)

        binding.code.setText(controllerSrc)
        binding.code.doAfterTextChanged { ed ->
            val src =  ed.toString()
            sharedPreferences.edit().putString("controller_src", src).apply()
            viewModel.updateControllerSrc(src)
        }
        val view = binding.root
        return view
    }

}