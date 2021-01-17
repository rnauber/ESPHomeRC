package dev.nauber.esphomerc

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData
import com.google.android.material.slider.Slider


class MyAuxControlRecyclerViewAdapter(
    private val values: LiveData<List<String>>,
    private val viewModel: ControlCommViewModel
) : RecyclerView.Adapter<MyAuxControlRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_aux_control, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val label = values.value?.get(position)
        holder.idView.text = label

        holder.contentView.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
                viewModel.updateInput(mapOf("aux$position" to slider.value))
            }
        })

    }

    override fun getItemCount(): Int = values.value?.size ?: 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.item_number)
        val contentView: Slider = view.findViewById(R.id.content)

        override fun toString(): String {
            return super.toString() + " " + idView.text + " '" + contentView.value + "'"
        }
    }
}