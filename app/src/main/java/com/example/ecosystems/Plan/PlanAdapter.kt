package com.example.ecosystems.Plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.R

class PlanAdapter(private val planList: MutableList<Plan>,
                  private val onItemClick: (Plan) -> Unit): RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val planName: TextView = itemView.findViewById(R.id.planName)
        val planDescription: TextView = itemView.findViewById(R.id.planDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.plan_item, parent,false)
        return PlanViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val currentItem = planList[position]
        holder.planName.text = currentItem.name
        val deviceDetails = currentItem.description
        holder.planDescription.text = deviceDetails

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return planList.size
    }
}