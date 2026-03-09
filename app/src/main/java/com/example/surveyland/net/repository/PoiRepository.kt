package com.example.surveyland.net.repository

import android.util.Log
import com.example.surveyland.entity.PoiItem2
import com.example.surveyland.net.manager.RetrofitSearchManager
import com.google.gson.Gson

class PoiRepository {

    private val api = RetrofitSearchManager.api

    suspend fun search(keyword: String, city: String): List<PoiItem2> {

        val response = api.searchPoi(
            keywords = keyword,
            city = city,
            key = "080b775fc13d65dd87e6f4cceaba680a"
        )
        Log.d("POI-RAW", Gson().toJson(response))
        return response.pois
    }

}