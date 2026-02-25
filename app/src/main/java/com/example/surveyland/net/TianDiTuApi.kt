package com.example.surveyland.net

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
