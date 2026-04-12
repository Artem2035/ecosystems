package com.example.ecosystems.Plan

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.R
import java.util.Locale

class PlanSearchDialogFragment(private var planList: MutableList<Plan>,
                               private var tempPlanList: MutableList<Plan>,
                               private val onConfirm: (plan: Plan) -> Unit) : DialogFragment()  {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_plan_search, null)


        val planRecyclerView: RecyclerView = view.findViewById(R.id.plans_recycler_view)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Выбрать план")
            .setView(view)
            .setNegativeButton("Отмена", null) // просто закроет диалог
            .create()

        val planAdapter = PlanAdapter(tempPlanList){ plan ->
            onConfirm(plan)
            dismiss()
        }
        planRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        planRecyclerView.adapter = planAdapter

        //поиск
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempPlanList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if(!searchText.isEmpty()){

                    planList.forEach {
                        if(it.name.lowercase(Locale.getDefault()).contains(searchText) ||
                            it.description.lowercase(Locale.getDefault()).contains(searchText)){
                            tempPlanList.add(it)
                        }
                    }

                    planAdapter.notifyDataSetChanged()
                }else{
                    tempPlanList.clear()
                    tempPlanList.addAll(planList)
                    planAdapter.notifyDataSetChanged()
                }
                return false
            }
        })

        dialog.show()

        val negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negative.setTextColor(Color.BLACK)

        val button = dialog.findViewById<ImageButton>(R.id.showMap)
        button?.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }
}