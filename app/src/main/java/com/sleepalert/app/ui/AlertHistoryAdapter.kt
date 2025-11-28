package com.sleepalert.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.sleepalert.app.R
import com.sleepalert.app.model.AlertRecord
import com.sleepalert.app.model.DrowsinessLevel
import java.text.SimpleDateFormat
import java.util.Locale

class AlertHistoryAdapter(
    private var items: List<AlertRecord>,
    private val onViewClick: (AlertRecord) -> Unit
) : RecyclerView.Adapter<AlertHistoryAdapter.AlertViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())

    inner class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLevelIcon: TextView = view.findViewById(R.id.tvLevelIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val btnView: MaterialButton = view.findViewById(R.id.btnView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = items[position]

        // icon + title theo level
        val (icon, titleText) = when (alert.level) {
            DrowsinessLevel.LOW -> "🙂" to "Cảnh báo mức THẤP"
            DrowsinessLevel.MEDIUM -> "😴" to "Cảnh báo mức TRUNG BÌNH"
            DrowsinessLevel.HIGH -> "💤" to "Cảnh báo mức CAO"
        }

        holder.tvLevelIcon.text = icon
        holder.tvTitle.text = titleText

        val timeStr = sdf.format(alert.timestamp)
        holder.tvSubtitle.text = "$timeStr · ${alert.durationSeconds} giây"

        holder.btnView.setOnClickListener { onViewClick(alert) }
    }

    fun updateData(newItems: List<AlertRecord>) {
        items = newItems
        notifyDataSetChanged()
    }
}
