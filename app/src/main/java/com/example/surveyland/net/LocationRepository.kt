package com.example.surveyland.net

import android.content.Context
import android.util.Log
import com.example.surveyland.R

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LocationRepository(private val context: Context) {

    private val token = context.getString(R.string.mapbox_access_token)

    fun getTownName(
        latitude: Double,
        longitude: Double,
        callback: (String?) -> Unit
    ) {

        val location = "$longitude,$latitude"  // 经度在前

        RetrofitManager.api.reverseGeocode(
            location = location,
            accessToken = token
        ).enqueue(object : Callback<GeocodeResponse> {

            override fun onResponse(
                call: Call<GeocodeResponse>,
                response: Response<GeocodeResponse>
            ) {

                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                val features = response.body()?.features

                if (features.isNullOrEmpty()) {
                    callback(null)
                    return
                }

                // 优先找 locality
                val town = features.firstOrNull {
                    it.place_type.contains("locality")
                }?.text

                // 如果没有 locality，用 district 兜底
                val result = town ?: features.firstOrNull {
                    it.place_type.contains("district")
                }?.text

                callback(result)
            }

            override fun onFailure(call: Call<GeocodeResponse>, t: Throwable) {
                Log.e("Mapbox", t.message ?: "error")
                callback(null)
            }
        })
    }
}