package com.example.ecosystems.TreesManagement

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.TreeRow
import com.example.ecosystems.R

class TreeAdapter(private val items: MutableList<TreeRow>) :
    RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    class TreeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var number: TextView = view.findViewById(R.id.colNumber)
        val species: TextView = view.findViewById(R.id.colSpecies)
        val age: TextView = view.findViewById(R.id.colAge)
        val d13: TextView = view.findViewById(R.id.colD13)
        val height: TextView = view.findViewById(R.id.colHeight)
        val lk: TextView = view.findViewById(R.id.colLk)
        val hDk: TextView = view.findViewById(R.id.colHDk)
        val crownDiameterNS: TextView = view.findViewById(R.id.colCrownDiameterNS)
        val crownDiameterEW: TextView = view.findViewById(R.id.colCrownDiameterEW)
        val averageCrownDiameter : TextView = view.findViewById(R.id.colAverageCrownDiameter )

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_tree_item, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val item = items[position]
        holder.number.text = item.number.toString()
        holder.species.text = item.species
        holder.age.text = item.age.toString()
        holder.d13.text = item.d13.toString()
        holder.height.text = item.height.toString()
        holder.lk.text = item.lk.toString()
        holder.hDk.text = item.hdk.toString()
        holder.crownDiameterNS.text = item.CrownDiameterNS.toString()
        holder.crownDiameterEW.text = item.CrownDiameterEW.toString()
        holder.averageCrownDiameter.text = item.AverageCrownDiameter .toString()
    }

    override fun getItemCount() = items.size

    fun addRow(row: TreeRow) {
        items.add(row)
        notifyItemInserted(items.size - 1)
        Log.d("addRow", "addRow")

    }
}
