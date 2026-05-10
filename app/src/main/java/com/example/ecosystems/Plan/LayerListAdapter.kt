package com.example.ecosystems.Plan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.LayerDisplayItem
import com.example.ecosystems.R

class LayerListAdapter(
    private val items: List<LayerDisplayItem>,
    private val onLayerClick: (LayerDisplayItem) -> Unit,
    private val onVisibilityToggle: (LayerDisplayItem, Boolean) -> Unit
) : RecyclerView.Adapter<LayerListAdapter.LayerViewHolder>() {

    class LayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layerColorView: View = itemView.findViewById(R.id.layerColorView)
        val layerNameText: TextView = itemView.findViewById(R.id.layerNameText)
        val layerTypeText: TextView = itemView.findViewById(R.id.layerTypeText)
        val visibilityCheckBox: CheckBox = itemView.findViewById(R.id.visibilityCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layer, parent, false)
        return LayerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        val item = items[position]

        holder.layerNameText.text = item.layer.name
        holder.layerTypeText.text = item.layer.type

        val color = runCatching {
            Color.parseColor(item.layer.color ?: "#FFA500")
        }.getOrDefault(Color.parseColor("#FFA500"))
        holder.layerColorView.setBackgroundColor(color)

        // кнопка видимости только для слоёв с точками и фото
        holder.visibilityCheckBox.visibility =
            if (item.layer.type == "points" || item.layer.type == "library_images") View.VISIBLE else View.GONE

        // Сначала снимаем старый listener, чтобы не срабатывал при recycling
        holder.visibilityCheckBox.setOnCheckedChangeListener(null)
        if(item.isVisible)
            holder.visibilityCheckBox.setButtonDrawable(R.drawable.outline_visibility_24)
        else
            holder.visibilityCheckBox.setButtonDrawable(R.drawable.outline_visibility_off_24)
        holder.visibilityCheckBox.isChecked = item.isVisible
        holder.visibilityCheckBox.setOnCheckedChangeListener { _, checked ->
            if(holder.visibilityCheckBox.isChecked)
                holder.visibilityCheckBox.setButtonDrawable(R.drawable.outline_visibility_24)
            else
                holder.visibilityCheckBox.setButtonDrawable(R.drawable.outline_visibility_off_24)
            item.isVisible = checked
            onVisibilityToggle(item, checked)
        }

        holder.itemView.setOnClickListener {
            onLayerClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}