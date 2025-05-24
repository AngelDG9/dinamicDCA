package com.dam.dinamicdca

import java.text.SimpleDateFormat
import java.util.*

data class Plan(
    var id: Long = 0,
    var nombre: String,
    var moneda: String,
    var athv: Double,
    var fechaAthv: String,
    var buyplan: String = "",
    var sellplan: String = "",
    var ticker: String = "",
    var athn: Double = 0.0,
    var price: Double = 0.0,
    var biggerdownfall: Double = 0.0,
    var actualperc: Double = 0.0
) {

    companion object {
        private val commonTickers = mapOf(
            "BTC-USD" to "Bitcoin",
            "ETH-USD" to "Ethereum",
            "ADA-USD" to "Cardano",
            "SOL-USD" to "Solana",
            "DOT-USD" to "Polkadot",
            "MATIC-USD" to "Polygon"
        )

        fun getDisplayName(ticker: String): String {
            return commonTickers[ticker] ?: ticker.replace("-USD", "")
        }
    }

    init {
        // Use moneda as ticker if ticker is empty
        if (ticker.isEmpty()) {
            ticker = moneda
        }
        // Keep moneda and ticker the same
        moneda = ticker
    }

    fun getBuyActions(): List<String> {
        val actions = mutableListOf<String>()
        if (buyplan.isEmpty() || athn == 0.0) return actions

        val ranges = buyplan.split(";")
        for (range in ranges) {
            val parts = range.split(",")
            when (parts.size) {
                2 -> {
                    val (rangeVal, amount) = parts.map { it.toDoubleOrNull() ?: 0.0 }
                    val threshold = rangeVal * athn

                    if (range == ranges.last()) {
                        if (price > threshold) {
                            actions.add("Comprar ${String.format("%.2f", amount)} €")
                        }
                    } else {
                        if (price < threshold) {
                            actions.add("Comprar ${String.format("%.2f", amount)} €")
                        }
                    }
                }
                3 -> {
                    val (range1, range2, amount) = parts.map { it.toDoubleOrNull() ?: 0.0 }
                    val threshold1 = range1 * athn
                    val threshold2 = range2 * athn

                    if (price >= threshold1 && price <= threshold2) {
                        actions.add("Comprar ${String.format("%.2f", amount)} €")
                    }
                }
            }
        }
        return actions
    }

    fun getSellActions(): List<String> {
        val actions = mutableListOf<String>()
        if (sellplan.isEmpty() || athv == 0.0) return actions

        val ranges = sellplan.split(";")
        var bestSellAction: String? = null

        for (range in ranges) {
            val parts = range.split(",")
            if (parts.size == 2) {
                val (rangeVal, percentage) = parts.map { it.toDoubleOrNull() ?: 0.0 }
                val threshold = rangeVal * athv

                if (price >= threshold) {
                    bestSellAction = "Vender ${String.format("%.2f", percentage * 100)} %"
                }
            }
        }

        bestSellAction?.let { actions.add(it) }
        return actions
    }

    fun getAllActions(): List<String> {
        return getBuyActions() + getSellActions()
    }
}