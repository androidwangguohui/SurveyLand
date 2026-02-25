package com.example.surveyland.net

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

object GeocodeHelper {

//    fun AppCompatActivity.getPlaceName(
//        latitude: Double,
//        longitude: Double,
//        callback: (String) -> Unit
//    ) {
//        lifecycleScope.launch {
//            try {
//                val response = RetrofitClient.geocodeApi.reverseGeocode(longitude, latitude, "pk.eyJ1Ijoiaml1dGVuZyIsImEiOiJjbWt0ZDJ0dGExbW9kM2xxbmx2eTJ1dzBnIn0.cKCFnOqjUPaOmZKu1F-h0A")
//                val placeName = response.features.firstOrNull()?.place_name ?: "未知位置"
//                callback(placeName)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                callback("未知位置")
//            }
//        }
//    }
}