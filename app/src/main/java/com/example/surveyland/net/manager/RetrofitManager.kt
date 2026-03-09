package com.example.surveyland.net.manager

import com.example.surveyland.net.api.MapboxApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitManager {

    private const val BASE_URL = "https://api.mapbox.com/"

    val api: MapboxApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MapboxApi::class.java)
    }

}