package com.example.expenceflow.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.api.RetrofitInstance
import kotlinx.coroutines.launch

class FuelViewModel : ViewModel() {

    var petrolPrice = mutableStateOf("Loading...")

    init {
        getPetrolPrice("Delhi")
    }

    private fun getPetrolPrice(city: String) {

        viewModelScope.launch {

            try {

                val response =
                    RetrofitInstance.api.getPetrolPrice(city)

                if (response.isSuccessful) {

                    petrolPrice.value =
                        response.body()?.price ?: "No Data"

                } else {

                    petrolPrice.value = "API Error"
                }

            } catch (e: Exception) {

                petrolPrice.value = e.message.toString()
            }
        }
    }
}