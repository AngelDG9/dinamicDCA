package com.dam.dinamicdca

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var planAdapter: PlanAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var yahooAPI: YahooFinanceAPI
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeComponents()
        setupRecyclerView()
        loadPlans()

        fabAdd.setOnClickListener {
            showCreatePlanDialog()
        }
    }

    private fun initializeComponents() {
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        databaseHelper = DatabaseHelper(this)
        yahooAPI = YahooFinanceAPI()

        // Setup refresh button
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            loadPlans()
        }
    }

    private fun setupRecyclerView() {
        planAdapter = PlanAdapter(
            plans = mutableListOf(),
            onPlanClick = { plan -> showPlanDetails(plan) },
            onPlanLongClick = { plan -> showEditPlanDialog(plan) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = planAdapter
        }
    }

    private fun loadPlans() {
        lifecycleScope.launch {
            try {
                val plans = databaseHelper.getAllPlans()
                // Actualizar los planes uno por uno para mantener el estado de la UI actualizado
                plans.forEach { plan ->
                    try {
                        val updatedPlan = yahooAPI.updatePlanData(plan)
                        planAdapter.updatePlan(updatedPlan)
                    } catch (e: Exception) {
                        // Si falla un plan individual, continuamos con el siguiente
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al cargar los planes: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCreatePlanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_plan, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etMoneda = dialogView.findViewById<EditText>(R.id.etMoneda)
        val etAthv = dialogView.findViewById<EditText>(R.id.etAthv)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        val buyPlanContainer = dialogView.findViewById<LinearLayout>(R.id.buyPlanContainer)
        val sellPlanContainer = dialogView.findViewById<LinearLayout>(R.id.sellPlanContainer)
        val btnAddBuyRule = dialogView.findViewById<Button>(R.id.btnAddBuyRule)
        val btnAddSellRule = dialogView.findViewById<Button>(R.id.btnAddSellRule)

        val buyRules = mutableListOf<String>()
        val sellRules = mutableListOf<String>()

        btnAddBuyRule.setOnClickListener {
            showAddBuyRuleDialog { rule -> 
                buyRules.add(rule)
                updateBuyRulesDisplay(buyPlanContainer, buyRules, btnAddBuyRule)
            }
        }

        btnAddSellRule.setOnClickListener {
            showAddSellRuleDialog { rule ->
                sellRules.add(rule)
                updateSellRulesDisplay(sellPlanContainer, sellRules, btnAddSellRule)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Crear Nuevo Plan")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                try {
                    val ticker = etMoneda.text.toString().trim().uppercase()
                    if (!ticker.endsWith("-USD")) {
                        Toast.makeText(this, "El par de trading debe terminar en -USD", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    val fechaAthv = String.format("%04d-%02d-%02d", 
                        datePicker.year,
                        datePicker.month + 1,
                        datePicker.dayOfMonth)

                    val plan = Plan(
                        nombre = etNombre.text.toString(),
                        moneda = ticker,
                        athv = etAthv.text.toString().toDouble(),
                        fechaAthv = fechaAthv,
                        buyplan = buyRules.joinToString(";"),
                        sellplan = sellRules.joinToString(";"),
                        ticker = ticker
                    )

                    val id = databaseHelper.insertPlan(plan)
                    if (id > 0) {
                        plan.id = id
                        loadPlans()
                        Toast.makeText(this, "Plan creado exitosamente", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al crear el plan: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateBuyRulesDisplay(container: LinearLayout, rules: List<String>, addButton: Button) {
        container.removeAllViews()
        
        rules.forEachIndexed { index, rule ->
            val parts = rule.split(",")
            val text = if (parts.size == 2) {
                "Comprar ${parts[1]}€ cuando el precio < ${parts[0]}x ATH"
            } else {
                "Comprar ${parts[2]}€ cuando el precio esté entre ${parts[0]}x y ${parts[1]}x ATH"
            }
            
            val ruleLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 8, 16, 8)
            }

            val textView = TextView(this).apply {
                this.text = text
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val deleteButton = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_delete)
                setOnClickListener {
                    val newRules = rules.toMutableList()
                    newRules.removeAt(index)
                    updateBuyRulesDisplay(container, newRules, addButton)
                }
            }

            ruleLayout.addView(textView)
            ruleLayout.addView(deleteButton)
            container.addView(ruleLayout)
        }

        // Create a new button instead of reusing the existing one
        val newAddButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            text = "Añadir Regla de Compra"
            setOnClickListener { addButton.performClick() }
        }
        container.addView(newAddButton)
    }

    private fun updateSellRulesDisplay(container: LinearLayout, rules: List<String>, addButton: Button) {
        container.removeAllViews()
        
        rules.forEachIndexed { index, rule ->
            val parts = rule.split(",")
            val text = "Vender ${String.format("%.1f", parts[1].toDouble() * 100)}% cuando el precio = ${parts[0]}x ATHV"
            
            val ruleLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 8, 16, 8)
            }

            val textView = TextView(this).apply {
                this.text = text
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val deleteButton = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_delete)
                setOnClickListener {
                    val newRules = rules.toMutableList()
                    newRules.removeAt(index)
                    updateSellRulesDisplay(container, newRules, addButton)
                }
            }

            ruleLayout.addView(textView)
            ruleLayout.addView(deleteButton)
            container.addView(ruleLayout)
        }

        // Create a new button instead of reusing the existing one
        val newAddButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            text = "Añadir Regla de Venta"
            setOnClickListener { addButton.performClick() }
        }
        container.addView(newAddButton)
    }

    private fun showAddBuyRuleDialog(onRuleAdded: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_buy_rule, null)
        val rangeType = dialogView.findViewById<RadioGroup>(R.id.rangeType)
        val singleRangeLayout = dialogView.findViewById<LinearLayout>(R.id.singleRangeLayout)
        val doubleRangeLayout = dialogView.findViewById<LinearLayout>(R.id.doubleRangeLayout)
        
        val etSingleRange = dialogView.findViewById<EditText>(R.id.etSingleRange)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etRangeFrom = dialogView.findViewById<EditText>(R.id.etRangeFrom)
        val etRangeTo = dialogView.findViewById<EditText>(R.id.etRangeTo)
        val etRangeAmount = dialogView.findViewById<EditText>(R.id.etRangeAmount)

        rangeType.setOnCheckedChangeListener { _, checkedId ->
            singleRangeLayout.visibility = if (checkedId == R.id.rbSingleRange) View.VISIBLE else View.GONE
            doubleRangeLayout.visibility = if (checkedId == R.id.rbDoubleRange) View.VISIBLE else View.GONE
        }

        AlertDialog.Builder(this)
            .setTitle("Añadir Regla de Compra")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                try {
                    val rule = if (rangeType.checkedRadioButtonId == R.id.rbSingleRange) {
                        "${etSingleRange.text},${etAmount.text}"
                    } else {
                        "${etRangeFrom.text},${etRangeTo.text},${etRangeAmount.text}"
                    }
                    onRuleAdded(rule)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al añadir regla", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddSellRuleDialog(onRuleAdded: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_sell_rule, null)
        val etMultiplier = dialogView.findViewById<EditText>(R.id.etMultiplier)
        val etPercentage = dialogView.findViewById<EditText>(R.id.etPercentage)

        AlertDialog.Builder(this)
            .setTitle("Añadir Regla de Venta")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                try {
                    val multiplier = etMultiplier.text.toString().toDouble()
                    val percentage = etPercentage.text.toString().toDouble() / 100.0
                    val rule = "$multiplier,$percentage"
                    onRuleAdded(rule)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al añadir regla", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditPlanDialog(plan: Plan) {
        val options = arrayOf(
            "Ver Detalles",
            "Modificar ATHV",
            "Modificar Fecha ATHV",
            "Modificar Plan de Compra",
            "Modificar Plan de Venta",
            "Eliminar Plan"
        )

        AlertDialog.Builder(this)
            .setTitle("Editar: ${plan.nombre}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPlanDetails(plan)
                    1 -> showEditAthvDialog(plan)
                    2 -> showEditFechaDialog(plan)
                    3 -> showEditBuyPlanDialog(plan)
                    4 -> showEditSellPlanDialog(plan)
                    5 -> showDeleteConfirmDialog(plan)
                }
            }
            .show()
    }

    private fun showPlanDetails(plan: Plan) {
        lifecycleScope.launch {
            val updatedPlan = yahooAPI.updatePlanData(plan)

            val details = StringBuilder().apply {
                appendLine("📊 DETALLES DEL PLAN")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("Nombre: ${updatedPlan.nombre}")
                appendLine("Moneda: ${updatedPlan.moneda} (${updatedPlan.ticker})")
                appendLine("ATHN: ${String.format("%.2f", updatedPlan.athn)}")
                appendLine("ATHV: ${String.format("%.2f", updatedPlan.athv)}")
                appendLine("Precio Actual: ${String.format("%.2f", updatedPlan.price)}")
                appendLine("% vs ATH: ${String.format("%.2f", updatedPlan.actualperc)}%")
                appendLine(
                    "Máxima caída desde ${updatedPlan.fechaAthv}: ${
                        String.format(
                            "%.2f",
                            updatedPlan.biggerdownfall
                        )
                    }%"
                )
                appendLine()

                if (updatedPlan.buyplan.isNotEmpty()) {
                    appendLine("📈 PLAN DE COMPRA (vs ATHN):")
                    val buyRanges = updatedPlan.buyplan.split(";")
                    buyRanges.forEach { range ->
                        val parts = range.split(",")
                        when (parts.size) {
                            2 -> {
                                val (rangeVal, amount) = parts.map { it.toDoubleOrNull() ?: 0.0 }
                                val threshold = rangeVal * updatedPlan.athn
                                val symbol = if (range == buyRanges.last()) ">" else "<"
                                appendLine(
                                    "• p $symbol ${
                                        String.format(
                                            "%.2f",
                                            threshold
                                        )
                                    } (${
                                        String.format(
                                            "%.2f",
                                            rangeVal
                                        )
                                    }): Comprar ${String.format("%.2f", amount)} €"
                                )
                            }

                            3 -> {
                                val (range1, range2, amount) = parts.map {
                                    it.toDoubleOrNull() ?: 0.0
                                }
                                val threshold1 = range1 * updatedPlan.athn
                                val threshold2 = range2 * updatedPlan.athn
                                appendLine(
                                    "• p ~ ${
                                        String.format(
                                            "%.2f",
                                            threshold1
                                        )
                                    }-${String.format("%.2f", threshold2)} (${
                                        String.format(
                                            "%.2f",
                                            range1
                                        )
                                    }-${String.format("%.2f", range2)}): Comprar ${
                                        String.format(
                                            "%.2f",
                                            amount
                                        )
                                    } €"
                                )
                            }
                        }
                    }
                    appendLine()
                }

                if (updatedPlan.sellplan.isNotEmpty()) {
                    appendLine("📉 PLAN DE VENTA (vs ATHV):")
                    val sellRanges = updatedPlan.sellplan.split(";")
                    sellRanges.forEach { range ->
                        val parts = range.split(",")
                        if (parts.size == 2) {
                            val (rangeVal, percentage) = parts.map { it.toDoubleOrNull() ?: 0.0 }
                            val threshold = rangeVal * updatedPlan.athv
                            appendLine(
                                "• p = ${
                                    String.format(
                                        "%.2f",
                                        threshold
                                    )
                                } (${String.format("%.2f", rangeVal)}): Vender ${
                                    String.format(
                                        "%.2f",
                                        percentage * 100
                                    )
                                } %"
                            )
                        }
                    }
                    appendLine()
                }

                val actions = updatedPlan.getAllActions()
                if (actions.isNotEmpty()) {
                    appendLine("🔔 ACCIONES RECOMENDADAS:")
                    actions.forEach { action ->
                        appendLine("• $action")
                    }
                }
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Plan: ${updatedPlan.nombre}")
                .setMessage(details.toString())
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun showEditAthvDialog(plan: Plan) {
        val input = EditText(this).apply {
            setText(plan.athv.toString())
            hint = "Nuevo ATHV"
        }

        AlertDialog.Builder(this)
            .setTitle("Modificar ATHV")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    plan.athv = input.text.toString().toDouble()
                    databaseHelper.updatePlan(plan)
                    loadPlans()
                    Toast.makeText(this, "ATHV actualizado", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: valor inválido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditFechaDialog(plan: Plan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_date, null)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)

        // Parse existing date to set initial values
        val dateParts = plan.fechaAthv.split("-")
        if (dateParts.size == 3) {
            datePicker.updateDate(
                dateParts[0].toInt(),  // year
                dateParts[1].toInt() - 1,  // month (0-based)
                dateParts[2].toInt()   // day
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Modificar Fecha ATHV")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newDate = String.format("%04d-%02d-%02d",
                    datePicker.year,
                    datePicker.month + 1,
                    datePicker.dayOfMonth)
                plan.fechaAthv = newDate
                databaseHelper.updatePlan(plan)
                loadPlans()
                Toast.makeText(this, "Fecha actualizada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditBuyPlanDialog(plan: Plan) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_buy_plan, null)
            val buyPlanContainer = dialogView.findViewById<LinearLayout>(R.id.buyPlanContainer)
                ?: throw IllegalStateException("buyPlanContainer not found")
            val btnAddBuyRule = dialogView.findViewById<Button>(R.id.btnAddBuyRule)
                ?: throw IllegalStateException("btnAddBuyRule not found")

            val buyRules = plan.buyplan.split(";").filter { it.isNotEmpty() }.toMutableList()

            // Display existing rules
            updateBuyRulesDisplay(buyPlanContainer, buyRules, btnAddBuyRule)

            btnAddBuyRule.setOnClickListener {
                showAddBuyRuleDialog { rule -> 
                    buyRules.add(rule)
                    updateBuyRulesDisplay(buyPlanContainer, buyRules, btnAddBuyRule)
                }
            }

            AlertDialog.Builder(this)
                .setTitle("Modificar Plan de Compra")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    try {
                        plan.buyplan = buyRules.joinToString(";")
                        databaseHelper.updatePlan(plan)
                        loadPlans()
                        Toast.makeText(this, "Plan de compra actualizado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el diálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showEditSellPlanDialog(plan: Plan) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_sell_plan, null)
            val sellPlanContainer = dialogView.findViewById<LinearLayout>(R.id.sellPlanContainer)
                ?: throw IllegalStateException("sellPlanContainer not found")
            val btnAddSellRule = dialogView.findViewById<Button>(R.id.btnAddSellRule)
                ?: throw IllegalStateException("btnAddSellRule not found")

            val sellRules = plan.sellplan.split(";").filter { it.isNotEmpty() }.toMutableList()

            // Display existing rules
            updateSellRulesDisplay(sellPlanContainer, sellRules, btnAddSellRule)

            btnAddSellRule.setOnClickListener {
                showAddSellRuleDialog { rule ->
                    sellRules.add(rule)
                    updateSellRulesDisplay(sellPlanContainer, sellRules, btnAddSellRule)
                }
            }

            AlertDialog.Builder(this)
                .setTitle("Modificar Plan de Venta")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    try {
                        plan.sellplan = sellRules.joinToString(";")
                        databaseHelper.updatePlan(plan)
                        loadPlans()
                        Toast.makeText(this, "Plan de venta actualizado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el diálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmDialog(plan: Plan) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar el plan '${plan.nombre}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                databaseHelper.deletePlan(plan.id)
                loadPlans()
                Toast.makeText(this, "Plan eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFullEditPlanDialog(plan: Plan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_plan, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etEditNombre)
        val etMoneda = dialogView.findViewById<EditText>(R.id.etEditMoneda)
        val etAthv = dialogView.findViewById<EditText>(R.id.etEditAthv)
        val etFechaAthv = dialogView.findViewById<EditText>(R.id.etEditFechaAthv)
        val etBuyPlan = dialogView.findViewById<EditText>(R.id.etEditBuyPlan)
        val etSellPlan = dialogView.findViewById<EditText>(R.id.etEditSellPlan)

        // Pre-llenar con los valores actuales
        etNombre.setText(plan.nombre)
        etMoneda.setText(plan.ticker) // Use ticker instead of moneda
        etAthv.setText(plan.athv.toString())
        etFechaAthv.setText(plan.fechaAthv)
        etBuyPlan.setText(plan.buyplan)
        etSellPlan.setText(plan.sellplan)

        AlertDialog.Builder(this)
            .setTitle("Editar Plan: ${plan.nombre}")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    val ticker = etMoneda.text.toString().trim().uppercase()
                    if (!ticker.endsWith("-USD")) {
                        Toast.makeText(this, "El par de trading debe terminar en -USD", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    plan.nombre = etNombre.text.toString()
                    plan.ticker = ticker
                    plan.moneda = Plan.getDisplayName(ticker)
                    plan.athv = etAthv.text.toString().toDouble()
                    plan.fechaAthv = etFechaAthv.text.toString()
                    plan.buyplan = etBuyPlan.text.toString()
                    plan.sellplan = etSellPlan.text.toString()

                    databaseHelper.updatePlan(plan)
                    loadPlans()
                    Toast.makeText(this, "Plan actualizado exitosamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}