package com.example.surveyland.net.api

import com.example.surveyland.entity.TianDiTuResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TianDiTuApi {

    @GET("geocoder")
    fun reverseGeocode(
        @Query("postStr") postStr: String,
        @Query("type") type: String = "geocode",
        @Query("tk") key: String
    ): Call<TianDiTuResponse>
}