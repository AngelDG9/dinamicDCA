package com.dam.dinamicdca

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Simplified API response classes
data class YahooResponse(
    val chart: Chart
)

data class Chart(
    val result: List<Result>
)

data class Result(
    val meta: Meta,
    val timestamp: List<Long>,
    val indicators: Indicators
)

data class Meta(
    val regularMarketPrice: Double
)

data class Indicators(
    val quote: List<Quote>
)

data class Quote(
    val close: List<Double?>
)

interface YahooFinanceService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getQuote(@Path("symbol") symbol: String): YahooResponse

    @GET("v8/finance/chart/{symbol}?range=max&interval=1d")
    suspend fun getHistoricalData(@Path("symbol") symbol: String): YahooResponse
}

class YahooFinanceAPI {

    private val service = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YahooFinanceService::class.java)

    suspend fun getCurrentPrice(ticker: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val response = service.getQuote(ticker)
                response.chart.result.first().meta.regularMarketPrice
            } catch (e: Exception) {
                0.0
            }
        }
    }

    suspend fun getATH(ticker: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val response = service.getHistoricalData(ticker)
                val result = response.chart.result.firstOrNull() ?: return@withContext 0.0
                val closes = result.indicators.quote.firstOrNull()?.close ?: return@withContext 0.0
                
                // Filtrar valores nulos y encontrar el máximo
                val validCloses = closes.filterNotNull()
                if (validCloses.isEmpty()) return@withContext 0.0
                
                // Encontrar el máximo histórico
                validCloses.maxOrNull() ?: 0.0
            } catch (e: Exception) {
                e.printStackTrace()
                0.0
            }
        }
    }

    suspend fun getMaxDrawdown(ticker: String, startDate: String): Double {
        return withContext(Dispatchers.IO) {
            try {
                val response = service.getHistoricalData(ticker)
                val result = response.chart.result.first()
                val timestamps = result.timestamp
                val closes = result.indicators.quote.first().close.filterNotNull()

                // Convert startDate to timestamp
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startTimestamp = dateFormat.parse(startDate)?.time?.div(1000) ?: 0

                // Filter data from start date
                val filteredPrices = mutableListOf<Double>()
                for (i in timestamps.indices) {
                    if (timestamps[i] >= startTimestamp && i < closes.size) {
                        filteredPrices.add(closes[i])
                    }
                }

                if (filteredPrices.isEmpty()) return@withContext 0.0

                // Calculate maximum drawdown
                var maxDrawdown = 0.0
                var peak = filteredPrices.first()

                for (price in filteredPrices) {
                    if (price > peak) {
                        peak = price
                    } else {
                        val drawdown = ((peak - price) / peak) * 100
                        if (drawdown > maxDrawdown) {
                            maxDrawdown = drawdown
                        }
                    }
                }

                maxDrawdown
            } catch (e: Exception) {
                0.0
            }
        }
    }

    suspend fun updatePlanData(plan: Plan): Plan {
        return withContext(Dispatchers.IO) {
            try {
                plan.price = getCurrentPrice(plan.ticker)
                plan.athn = getATH(plan.ticker)
                plan.biggerdownfall = getMaxDrawdown(plan.ticker, plan.fechaAthv)
                plan.actualperc = if (plan.athn > 0) (plan.price / plan.athn) * 100 else 0.0
                plan
            } catch (e: Exception) {
                plan
            }
        }
    }
}