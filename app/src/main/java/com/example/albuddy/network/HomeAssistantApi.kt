package com.example.albuddy.network

import com.example.albuddy.network.model.HAEntity
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HomeAssistantApi {
    @GET("api/states")
    suspend fun getStates(): List<HAEntity>

    @GET("api/services")
    suspend fun getServices(): List<com.example.albuddy.network.model.HAServiceDomain>

    @POST("api/services/{domain}/{service}")
    suspend fun callService(
        @Path("domain") domain: String,
        @Path("service") service: String,
        @Body body: Map<String, String>
    ): List<HAEntity>
}
