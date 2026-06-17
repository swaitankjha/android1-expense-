package com.example.expenceflow.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL =
        "https://fuel-petrol-diesel-live-price-india.p.rapidapi.com/"

    val api: FuelApiService by lazy {

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->

                val request = chain.request().newBuilder()

                    // CHANGE THIS
                    .addHeader(
                        "x-rapidapi-key",
                        "e1255cf8a7mshdd527f03a311f8cp11e390jsnc7ee89155839"
                    )

                    // DONT CHANGE THIS
                    .addHeader(
                        "x-rapidapi-host",
                        "fuel-petrol-diesel-live-price-india.p.rapidapi.com"
                    )
                    .build()

                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()

            // DONT CHANGE THIS
            .baseUrl(BASE_URL)

            .client(client)

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(FuelApiService::class.java)
    }
}