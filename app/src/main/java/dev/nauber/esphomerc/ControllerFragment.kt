package dev.nauber.esphomerc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.brackeys.ui.editorkit.utils.UndoStack
import com.brackeys.ui.language.javascript.JavaScriptLanguage
import dev.nauber.esphomerc.databinding.FragmentControllerBinding

class ControllerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentControllerBinding.inflate(inflater, container, false)
        val viewModel: ControlCommViewModel by viewModels({ requireActivity() })

        val editor = binding.code
        editor.language = JavaScriptLanguage()

        editor.setTextContent(viewModel.controllerSrc)

        viewModel.liveControllerSrc.observe(this, {
            editor.setTextContent(it)
        })
        //TODO keep the undo stack
        editor.undoStack = UndoStack()
        editor.redoStack = UndoStack()

        binding.scroller.attachTo(editor)
        binding.code
        binding.code.doAfterTextChanged { ed ->
            val src = ed.toString()
            viewModel.controllerSrc = src
        }
        val view = binding.root
        return view
    }

}