package com.example.ecosystems

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.TreeRow
import com.example.ecosystems.TreesManagement.TreeAdapter

class TreesManagementActivity : AppCompatActivity() {
    private lateinit var adapter: TreeAdapter
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trees_management)

        val recycler = findViewById<RecyclerView>(R.id.tableRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = TreeAdapter(mutableListOf())
        recycler.adapter = adapter

        val addTreeButton: Button = findViewById(com.example.ecosystems.R.id.addTreeButton)
        addTreeButton.setOnClickListener {
            showAddTreeDialog()
        }
    }

    fun startForestTaxationActivity(view: View)
    {
        val intent =  Intent(this,ForestTaxationActivity::class.java)
        startActivity(intent)
    }

    private fun showAddTreeDialog() {
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_tree, null)

        val speciesEt = dialogView.findViewById<EditText>(R.id.speciesEt)
        val ageEt = dialogView.findViewById<EditText>(R.id.ageEt)
        val d13Et = dialogView.findViewById<EditText>(R.id.d13Et)
        val heightEt = dialogView.findViewById<EditText>(R.id.heightEt)
        val lkEt = dialogView.findViewById<EditText>(R.id.lkEt)
        val hdkEt = dialogView.findViewById<EditText>(R.id.hdkEt)
        val crownDiameterNSEt = dialogView.findViewById<EditText>(R.id.crownDiameterNSEt)
        val crownDiameterEWEt = dialogView.findViewById<EditText>(R.id.crownDiameterEWEt)
        val averageCrownDiameterEt = dialogView.findViewById<EditText>(R.id.averageCrownDiameterEt)

        val builder = AlertDialog.Builder(this)
            .setTitle("Добавить дерево")
            .setView(dialogView)
            .setNegativeButton("Отмена", null) // просто закроет диалог

        // создаём, показываем, затем заменяем стандартный слушатель Positive, чтобы не закрывать диалог при ошибке валидации
        val dialog = builder.create()
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Добавить") { _, _ -> /* placeholder */ }
        dialog.show()

        val negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negative.setTextColor(Color.BLACK)
        // Получаем кнопку и переопределяем поведение
        val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positive.setTextColor(Color.WHITE)
        positive.background = ContextCompat.getDrawable(this, R.drawable.rounded_corners_button)
        positive.backgroundTintList = null
        positive.minHeight = 0
        positive.minimumHeight = 0
        positive.minWidth = 0
        positive.minimumWidth = 0
        positive.setPadding(30, 15, 30, 15)

        positive.setOnClickListener {
            val species = speciesEt.text.toString().trim()
            val ageStr = ageEt.text.toString().trim()
            val d13Str = d13Et.text.toString().trim()
            val heightStr = heightEt.text.toString().trim()

            val lkStr = lkEt.text.toString().trim()
            val hdkStr = hdkEt.text.toString().trim()
            val crownDiameterNSStr = crownDiameterNSEt.text.toString().trim()
            val crownDiameterEWStr = crownDiameterEWEt.text.toString().trim()
            val averageCrownDiameterStr = averageCrownDiameterEt.text.toString().trim()

            // Валидация
            if (species.isEmpty()) {
                speciesEt.error = "Введите породу"
                speciesEt.requestFocus()
                return@setOnClickListener
            }
            val age = ageStr.toIntOrNull()
            if (age == null || age < 0) {
                ageEt.error = "Неверный возраст"
                ageEt.requestFocus()
                return@setOnClickListener
            }
            val d13 = d13Str.toDoubleOrNull()
            if (d13 == null || d13 < 0.0) {
                d13Et.error = "Неверный диаметр ствола на высоте 1,3 м от земли"
                d13Et.requestFocus()
                return@setOnClickListener
            }
            val height = heightStr.toDoubleOrNull()
            if (height == null || height < 0.0) {
                heightEt.error = "Неверная высота"
                heightEt.requestFocus()
                return@setOnClickListener
            }
            val lk = lkStr.toDoubleOrNull()
            if (lk == null || lk < 0.0) {
                lkEt.error = "длина кроны дерева"
                lkEt.requestFocus()
                return@setOnClickListener
            }
            val hdk = hdkStr.toDoubleOrNull()
            if (hdk == null || hdk < 0.0) {
                hdkEt.error = "Неверная высота до начала кроны"
                hdkEt.requestFocus()
                return@setOnClickListener
            }

            val crownDiameterNS = crownDiameterNSStr.toDoubleOrNull()
            if (crownDiameterNS == null || crownDiameterNS < 0.0) {
                crownDiameterNSEt.error = "Неверный диаметр кроны дерева по оси север–юг"
                crownDiameterNSEt.requestFocus()
                return@setOnClickListener
            }

            val crownDiameterEW = crownDiameterEWStr.toDoubleOrNull()
            if (crownDiameterEW == null || crownDiameterEW < 0.0) {
                crownDiameterEWEt.error = "Неверный диаметр кроны по оси запад–восток"
                crownDiameterEWEt.requestFocus()
                return@setOnClickListener
            }

            val averageCrownDiameter = averageCrownDiameterStr.toDoubleOrNull()
            if (averageCrownDiameter == null || averageCrownDiameter < 0.0) {
                averageCrownDiameterEt.error = "Неверное среднее значение диаметров кроны"
                averageCrownDiameterEt.requestFocus()
                return@setOnClickListener
            }

            val num = adapter.itemCount
            adapter.addRow(TreeRow( num + 1, species = species, age = age, d13 = d13, height = height,
                lk=lk, hdk, crownDiameterNS, crownDiameterEW,averageCrownDiameter))

            dialog.dismiss()
        }
    }
}
