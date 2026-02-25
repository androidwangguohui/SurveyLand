package com.example.map_amap.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.LatLng

class AMapMeasureActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapView = MapView(this)
        setContentView(mapView)

        mapView.onCreate(savedInstanceState)

        val aMap = mapView.map
        aMap.isMyLocationEnabled = true
        aMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(39.9042, 116.4074), // 北京
                15f
            )
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
