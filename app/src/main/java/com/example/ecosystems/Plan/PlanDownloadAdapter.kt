package com.example.ecosystems.Plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.PlanDownloadItem
import com.example.ecosystems.R

class PlanDownloadAdapter(
    private val items: MutableList<PlanDownloadItem>,
    private val onDownload: (item: PlanDownloadItem, position: Int) -> Unit
) : RecyclerView.Adapter<PlanDownloadAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView      = view.findViewById(R.id.planNameText)
        val descText: TextView      = view.findViewById(R.id.planDescText)
        val downloadedIcon: ImageView = view.findViewById(R.id.downloadedIcon)
        val progressBar: ProgressBar  = view.findViewById(R.id.itemProgressBar)
        val downloadButton: Button    = view.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan_download, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.nameText.text = item.plan.name
        holder.descText.text = item.plan.description

        when {
            // Идёт скачивание — спиннер вместо кнопки
            item.isLoading -> {
                holder.progressBar.visibility = View.VISIBLE
                holder.downloadButton.visibility  = View.GONE
                holder.downloadedIcon.visibility  = View.GONE
            }
            // Скачан — галочка + кнопка «Обновить»
            item.isDownloaded -> {
                holder.progressBar.visibility = View.GONE
                holder.downloadButton.visibility  = View.VISIBLE
                holder.downloadButton.text        = "Обновить"
                holder.downloadedIcon.visibility  = View.VISIBLE
            }
            // Не скачан
            else -> {
                holder.progressBar.visibility = View.GONE
                holder.downloadButton.visibility  = View.VISIBLE
                holder.downloadButton.text        = "Скачать"
                holder.downloadedIcon.visibility  = View.GONE
            }
        }

        holder.downloadButton.setOnClickListener {
            // bindingAdapterPosition — актуальная позиция даже после notifyItemChanged
            onDownload(item, holder.adapterPosition)
        }
    }
}