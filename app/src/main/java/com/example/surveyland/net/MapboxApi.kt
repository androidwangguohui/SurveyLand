package com.example.surveyland.net

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MapboxApi {

    @GET("geocoding/v5/mapbox.places/{location}.json")
    fun reverseGeocode(// @Query("types") types: String = "locality,district",
        @Path("location") location: String,
        @Query("language") language: String = "zh-Hans",
        @Query("access_token") accessToken: String
    ): Call<GeocodeResponse>
}