package com.example.surveyland.net.api

import com.example.surveyland.entity.GeoResponse
import com.example.surveyland.entity.PoiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AMapApi {

    @GET("v3/place/text")
    suspend fun searchPoi(
        @Query("keywords") keywords: String,
        @Query("city") city: String,
        @Query("key") key: String
    ): PoiResponse


    @GET("v3/geocode/geo")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") key: String
    ): GeoResponse
}