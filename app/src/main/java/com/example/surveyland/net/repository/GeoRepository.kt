package com.example.surveyland.net.repository

import com.example.surveyland.entity.GeoItem
import com.example.surveyland.net.manager.RetrofitSearchManager

class GeoRepository {

    private val api = RetrofitSearchManager.api

    suspend fun search(address: String): GeoItem? {

        val result = api.geocode(
            address,
            "875a64be68b38722847e25cea5eb5a8d"
        )

        return result.geocodes.firstOrNull()
    }

}