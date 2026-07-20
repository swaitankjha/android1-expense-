package com.example.expenceflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import android.util.Log
/* ---------------- MODELS ---------------- */

data class MarketResponse(
    val success: Boolean,
    val gold: GoldData,
    val silver: SilverData,
    val fuel: FuelData,
    val updatedAt: String
)

data class GoldData(
    val gold24: Double,
    val gold22: Double,
    val gold18: Double
)

data class SilverData(
    val gram: Double,
    val kg: Double
)

data class FuelData(
    val petrol: Double,
    val diesel: Double
)

/* ---------------- API ---------------- */

interface MarketApi {

    @GET("market")
    suspend fun getMarketData(): MarketResponse
}

/* ---------------- UI STATE ---------------- */

sealed class MarketUiState {

    object Loading : MarketUiState()

    data class Success(
        val data: MarketResponse
    ) : MarketUiState()

    data class Error(
        val message: String
    ) : MarketUiState()
}

/* ---------------- VIEWMODEL ---------------- */

class LiveRatesViewModel : ViewModel() {

    private val _marketState =
        MutableStateFlow<MarketUiState>(
            MarketUiState.Loading
        )

    val marketState =
        _marketState.asStateFlow()

    private val _isRefreshing =
        MutableStateFlow(false)

    val isRefreshing =
        _isRefreshing.asStateFlow()

    private val api =
        Retrofit.Builder()
            .baseUrl("https://monitoringserch.onrender.com/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(MarketApi::class.java)

    init {
        // Only fetch if we don't have data yet
        if (_marketState.value is MarketUiState.Loading) {
            refreshRates()
        }
    }

    fun refreshRates(force: Boolean = false) {
        if (!force && _isRefreshing.value) return
        
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                // If it's a manual refresh (force), we might want to keep the old data visible
                if (force) {
                    // Stay in success but show loading indicator via _isRefreshing
                } else {
                    _marketState.value = MarketUiState.Loading
                }

                val response =
                    api.getMarketData()

                Log.d(
                    "MARKET_API",
                    response.toString()
                )

                if (response.success) {

                    _marketState.value =
                        MarketUiState.Success(
                            response
                        )

                } else {

                    _marketState.value =
                        MarketUiState.Error(
                            "API returned success=false"
                        )
                }

            } catch (e: Exception) {

                Log.e(
                    "MARKET_API",
                    "Request Failed",
                    e
                )

                _marketState.value =
                    MarketUiState.Error(
                        e.localizedMessage ?: "Unknown Error"
                    )

            } finally {

                _isRefreshing.value = false
            }
        }
    }
}