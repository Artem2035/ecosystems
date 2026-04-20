package com.example.ecosystems

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.Dialogs.PointDataDialogFragment
import com.example.ecosystems.db.AppDatabase
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.dao.TableEntityDao
import com.example.ecosystems.db.entity.table.TablePropertyEntity
import com.example.ecosystems.db.relation.LayerPointWithValues
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import com.example.ecosystems.db.repository.TableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TreesManagementActivity : AppCompatActivity() {
    private var planId: Int = 0

    private lateinit var headerRow: LinearLayout
    private lateinit var tableBody: LinearLayout

    private lateinit var layerDao: LayerEntityDao
    private lateinit var layerRepository: LayerRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var planDao: PlanEntityDao
    private lateinit var tableRepository: TableRepository
    private lateinit var tableDao: TableEntityDao

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trees_management)

        layerDao = AppDatabase.getInstance(this).layerDao()
        planDao = AppDatabase.getInstance(this).planDao()
        tableDao = AppDatabase.getInstance(this).tableDao()
        layerRepository = LayerRepository(layerDao)
        planRepository = PlanRepository(planDao)
        tableRepository = TableRepository(tableDao)

        planId =  intent.extras?.getSerializable("planId").toString().toInt()

        headerRow = findViewById(R.id.headerRow)
        tableBody = findViewById(R.id.tableBody)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadTable()
    }

    private fun loadTable() {
        lifecycleScope.launch {
            val plan = withContext(Dispatchers.IO) { planRepository.getPlanData(planId) }
            plan.layers.forEach {layer->
                when(layer.type){
                    "points" -> {
                        if(layer.tableId == null)
                            return@forEach

                        // 1. Свойства таблицы — заголовки колонок
                        val tableWithProperties = withContext(Dispatchers.IO) {
                            tableRepository.getTableWithProperties(layer.tableId)
                        }
                        val properties = tableWithProperties?.properties
                            ?.sortedBy { it.sortOrder }
                            ?: emptyList()

                        buildHeader(properties)

                        // 2. Все точки слоя
                        layerRepository.getPointsWithValuesByLayerId(layer.id)
                            ?.flowOn(Dispatchers.IO)
                            ?.collect { points ->
                                tableBody.removeAllViews()
                                points.forEach { point ->
                                    buildRow(point, properties)
                                }
                            }
                    }
                }
            }

        }
    }

    private var columnWidth = 0
    private fun buildHeader(properties: List<TablePropertyEntity>) {
        val totalColumns = 3 + properties.size

        val screenWidth = resources.displayMetrics.widthPixels
        val minColumnWidth = dpToPx(120)
        val expandedColumnWidth = screenWidth / totalColumns

        // Если колонки влезают — растягиваем, иначе фиксированная минимальная ширина
        columnWidth = if (expandedColumnWidth >= minColumnWidth) {
            expandedColumnWidth
        } else {
            minColumnWidth
        }

        headerRow.removeAllViews()
        // Фиксированные колонки
        headerRow.addView(makeHeaderCell("№"))
        headerRow.addView(makeHeaderCell("Широта"))
        headerRow.addView(makeHeaderCell("Долгота"))

        // Динамические колонки из свойств таблицы
        properties.forEach { property ->
            val title = property.displayName
                ?.takeIf { it.isNotBlank() }
                ?: property.name
            headerRow.addView(makeHeaderCell(title))
        }
    }

    private fun buildRow(
        item: LayerPointWithValues,
        properties: List<TablePropertyEntity>
    ) {
        val valuesMap = item.values.associate { it.value.propertyId to it.value.value }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                // открыть диалог или детальное view по точке
                PointDataDialogFragment(item.point.id, layerRepository, tableRepository)
                    .show(supportFragmentManager, "point_data")
            }
        }

        // Фиксированные ячейки
        row.addView(makeCell(item.point.num.toString()))
        row.addView(makeCell("%.6f".format(item.point.lat)))
        row.addView(makeCell("%.6f".format(item.point.lng)))

        // Динамические ячейки
        properties.forEach { property ->
            val value = valuesMap[property.id]
                ?.takeIf { it.isNotBlank() }
                ?: "—"
            val units = property.units.takeIf { it.isNotBlank() }
            val display = if (units != null && value != "—") "$value $units" else value
            row.addView(makeCell(display))
        }

        // Разделитель между строками
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
            )
            setBackgroundColor(ContextCompat.getColor(this@TreesManagementActivity, R.color.gray))
        }

        tableBody.addView(row)
        tableBody.addView(divider)
    }

    private fun makeHeaderCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@TreesManagementActivity, R.color.black))
            layoutParams = LinearLayout.LayoutParams(columnWidth, LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
    }

    private fun makeCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@TreesManagementActivity, R.color.black))
            layoutParams = LinearLayout.LayoutParams(columnWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun startForestTaxationActivity(view: View)
    {
        val intent =  Intent(this,ForestTaxationActivity::class.java)
        startActivity(intent)
    }
}
