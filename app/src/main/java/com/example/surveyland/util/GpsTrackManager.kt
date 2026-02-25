package com.example.surveyland.util

import android.Manifest
import android.content.Context
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.example.surveyland.entity.LatLngPoint
import com.example.surveyland.manage.DrawManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point

class GpsTrackManager(
    private val context: Context,
    private val drawManager: DrawManager
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val track = mutableListOf<Point>()

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()

        client.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    val point = Point.fromLngLat(loc.longitude, loc.latitude)
                    track.add(point)
                    drawManager.addPoint(point)
                }
            },
            Looper.getMainLooper()
        )
    }

    fun stopAndCalculate(): Double {
        return GeoMeasureUtil.area(track)
    }
}
