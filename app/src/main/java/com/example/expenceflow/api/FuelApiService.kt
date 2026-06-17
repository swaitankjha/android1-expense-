package com.example.expenceflow.api
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FuelApiService {

    @GET("petrolPrice")
    suspend fun getPetrolPrice(
        @Query("city") city: String
    ): Response<PetrolResponse>
}