package com.example.surveyland.util

import android.location.Location
import com.mapbox.geojson.Point

class GpsTrackCollector {

    private val points = mutableListOf<Point>()

    fun add(location: Location) {
        location.accuracy.let { if (it > 5) return }   // 精度过滤
        points.add(
            Point.fromLngLat(location.longitude, location.latitude)
        )
    }

    fun get(): List<Point> = points
}
