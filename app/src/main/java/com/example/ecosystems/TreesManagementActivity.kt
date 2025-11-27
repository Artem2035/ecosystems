package com.example.ecosystems

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.TreeRow
import com.example.ecosystems.TreesManagement.TreeAdapter
import com.google.android.play.integrity.internal.y

class TreesManagementActivity : AppCompatActivity() {
    private lateinit var adapter: TreeAdapter
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trees_management)

        val recycler = findViewById<RecyclerView>(R.id.tableRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

//        val t= findViewById<TextView>(R.id.head1)
//        t.post { Log.d("width", "Real width after layout: ${t.width} px") }
//        val t2= findViewById<TextView>(R.id.head2)
//        t2.post { Log.d("width", "Real width after layout: ${t2.width} px") }
//        val t3= findViewById<TextView>(R.id.head3)
//        t3.post { Log.d("width", "Real width after layout: ${t3.width} px") }
//        val t4= findViewById<TextView>(R.id.head4)
//        t4.post { Log.d("width", "Real width after layout: ${t4.width} px") }
//        val t5= findViewById<TextView>(R.id.head5)
//        t5.post { Log.d("width", "Real width after layout: ${t5.width} px") }
//        val t6= findViewById<TextView>(R.id.head6)
//        t6.post { Log.d("width", "Real width after layout: ${t6.width} px") }
//        val t7= findViewById<TextView>(R.id.head7)
//        t7.post { Log.d("width", "Real width after layout: ${t7.width} px") }
//        val t8= findViewById<TextView>(R.id.head8)
//        t8.post { Log.d("width", "Real width after layout: ${t8.width} px") }
//        val t9= findViewById<TextView>(R.id.head9)
//        t9.post { Log.d("width", "Real width after layout: ${t9.width} px") }
//        val t10= findViewById<TextView>(R.id.head10)
//        t10.post { Log.d("width", "Real width after layout: ${t10.width} px") }


//        Log.d("width", "${findViewById<TextView>(R.id.head1).measuredWidth}")
//        Log.d("width1", "${findViewById<TextView>(R.id.head2).measuredWidth}")
//        Log.d("width2", "${findViewById<TextView>(R.id.head3).measuredWidth}")
//        Log.d("width3", "${findViewById<TextView>(R.id.head4).measuredWidth}")
//        Log.d("width4", "${findViewById<TextView>(R.id.head5).measuredWidth}")
//        Log.d("width5", "${findViewById<TextView>(R.id.head6).measuredWidth}")
//        Log.d("width6", "${findViewById<TextView>(R.id.head7).measuredWidth}")
//        Log.d("width7", "${findViewById<TextView>(R.id.head8).measuredWidth}")
//        Log.d("width8", "${findViewById<TextView>(R.id.head9).measuredWidth}")
//        Log.d("width9", "${findViewById<TextView>(R.id.head10).measuredWidth}")

        adapter = TreeAdapter(mutableListOf())
        recycler.adapter = adapter

        val addTreeButton: Button = findViewById(com.example.ecosystems.R.id.addTreeButton)
        addTreeButton.setOnClickListener {
            val num = adapter.itemCount
            Log.d("TEST", "items=${adapter.itemCount}, visibleChildren=${recycler.childCount}")

            adapter.addRow(TreeRow( num + 1, species = "Сосна", age = 25, d13 = 270.0, height = 12.5,
                lk=8.0, 1.23, 3.45, 3.24,3.345))
            adapter.notifyDataSetChanged()
        }
    }

    fun startForestTaxationActivity(view: View)
    {
        val intent =  Intent(this,ForestTaxationActivity::class.java)
        startActivity(intent)
    }
}
