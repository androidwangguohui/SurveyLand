package com.example.surveyland.net

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TianDiTuRetrofit {

    private const val BASE_URL = "https://api.tianditu.gov.cn/"

    val api: TianDiTuApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TianDiTuApi::class.java)
    }
}