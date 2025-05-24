package com.dam.dinamicdca

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class PlanAdapter(
    private var plans: MutableList<Plan>,
    private val onPlanClick: (Plan) -> Unit,
    private val onPlanLongClick: (Plan) -> Unit
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    private val decimalFormat = DecimalFormat("#,##0.00")

    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvMoneda: TextView = view.findViewById(R.id.tvMoneda)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val tvActions: TextView = view.findViewById(R.id.tvActions)
        val tvAthv: TextView = view.findViewById(R.id.tvAthv)
        val tvAthn: TextView = view.findViewById(R.id.tvAthn)
        val tvMaxDrawdown: TextView = view.findViewById(R.id.tvMaxDrawdown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]

        holder.tvNombre.text = plan.nombre
        holder.tvMoneda.text = "${plan.moneda} (${plan.ticker})"

        if (plan.price > 0) {
            holder.tvPrice.text = "${decimalFormat.format(plan.price)}"
            holder.tvPercentage.text = "${decimalFormat.format(plan.actualperc)}% vs ATH"
        } else {
            holder.tvPrice.text = "Cargando..."
            holder.tvPercentage.text = ""
        }

        holder.tvAthv.text = "ATHV: ${decimalFormat.format(plan.athv)}"
        holder.tvAthn.text = "ATHN: ${decimalFormat.format(plan.athn)}"
        holder.tvMaxDrawdown.text = "Max caída: ${decimalFormat.format(plan.biggerdownfall)}%"

        // Show actions
        val actions = plan.getAllActions()
        if (actions.isNotEmpty()) {
            holder.tvActions.text = "🔔 ${actions.joinToString(" | ")}"
            holder.tvActions.visibility = View.VISIBLE
        } else {
            holder.tvActions.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onPlanClick(plan) }
        holder.itemView.setOnLongClickListener {
            onPlanLongClick(plan)
            true
        }
    }

    override fun getItemCount() = plans.size

    fun updatePlans(newPlans: List<Plan>) {
        plans.clear()
        plans.addAll(newPlans)
        notifyDataSetChanged()
    }

    fun updatePlan(updatedPlan: Plan) {
        val index = plans.indexOfFirst { it.id == updatedPlan.id }
        if (index != -1) {
            plans[index] = updatedPlan
            notifyItemChanged(index)
        } else {
            // Si el plan no existe, lo añadimos
            plans.add(updatedPlan)
            notifyItemInserted(plans.size - 1)
        }
    }
}