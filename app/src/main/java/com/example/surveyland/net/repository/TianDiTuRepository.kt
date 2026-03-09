package com.example.surveyland.net.repository

import android.content.Context
import com.example.surveyland.entity.TianDiTuResponse
import com.example.surveyland.net.TianDiTuRetrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TianDiTuRepository(private val context: Context) {

    private val key = "18200bf5ba2f674c772624185d27c1c9"

    fun getVillageName(
        latitude: Double,
        longitude: Double,
        callback: (String?) -> Unit
    ) {

        val postStr = """
            {"lon":$longitude,"lat":$latitude,"ver":1}
        """.trimIndent()

        TianDiTuRetrofit.api.reverseGeocode(
            postStr = postStr,
            key = key
        ).enqueue(object : Callback<TianDiTuResponse> {

            override fun onResponse(
                call: Call<TianDiTuResponse?>,
                response: Response<TianDiTuResponse?>
            ) {
                if (!response.isSuccessful) {
                    callback(null)
                    return
                }

                val component = response.body()?.result?.addressComponent

                // 优先返回行政村
                val village = component?.village

                // 没有行政村则返回乡镇
                val town = component?.town

                callback(village ?: town)
            }

            override fun onFailure(call: Call<TianDiTuResponse>, t: Throwable) {
                callback(null)
            }
        })
    }
}