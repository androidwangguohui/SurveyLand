package com.example.surveyland.net.manager

import com.example.surveyland.net.api.AMapApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object  RetrofitSearchManager {
    val api: AMapApi by lazy {

        Retrofit.Builder()
            .baseUrl("https://restapi.amap.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AMapApi::class.java)
    }
}