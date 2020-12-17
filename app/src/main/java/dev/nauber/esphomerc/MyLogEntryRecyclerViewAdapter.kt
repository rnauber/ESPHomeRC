package dev.nauber.esphomerc

import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LiveData

class MyLogEntryRecyclerViewAdapter(
    private val values: LiveData<List<LogItem>>
) : RecyclerView.Adapter<MyLogEntryRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_logentry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values.value?.get(position)
        if (item != null) {
            val source = item.source
            holder.idView.text = ""
            when (source) {
                Controller.LOGTAG -> holder.idView.setBackgroundResource(R.drawable.ic_logo2)
                Communication.LOGTAGESPHOME -> holder.idView.setBackgroundResource(R.drawable.ic_esphome_logo)
                Communication.LOGTAG -> holder.idView.setBackgroundResource(R.drawable.ic_baseline_wifi_24)
                else -> holder.idView.text = source
            }
            holder.contentView.text = item.content
        }

    }

    override fun getItemCount(): Int = values.value?.size ?: 0

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.symbol)
        val contentView: TextView = view.findViewById(R.id.content)

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}