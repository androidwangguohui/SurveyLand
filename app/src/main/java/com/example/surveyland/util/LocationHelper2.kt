package com.example.surveyland.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.surveyland.ui.view.AppToast
import com.google.android.gms.location.*

class LocationHelper2(
    private val context: Context,
    private val activity: Activity
) {

    // 回调接口
    interface LocationListener {
        fun onLocation(latitude: Double, longitude: Double, success: Boolean)
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * 检查权限和定位开关
     * 如果权限允许，直接启动定位
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startContinuousLocation(listener: LocationListener, once: Boolean = false) {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!isLocationEnabled()) {
            AppToast.show(activity, "请打开定位开关")
            listener.onLocation(0.0, 0.0, false)
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5秒间隔
        ).setMinUpdateIntervalMillis(2000L) // 最小间隔 2秒
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    listener.onLocation(loc.latitude, loc.longitude, true)
                    if (once) {
                        stopContinuousLocation()
                    }
                } else {
                    listener.onLocation(0.0, 0.0, false)
                }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /**
     * 单次定位，更简单稳定
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getSingleLocation(listener: LocationListener) {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        if (!isLocationEnabled()) {
            AppToast.show(activity, "请打开定位开关")
            listener.onLocation(0.0, 0.0, false)
            return
        }

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    listener.onLocation(location.latitude, location.longitude, true)
                } else {
                    listener.onLocation(0.0, 0.0, false)
                }
            }
            .addOnFailureListener {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = locationManager.getProviders(true)
                var bestLocation: Location? = null
                for (provider in providers) {
                    val l = locationManager.getLastKnownLocation(provider)
                    if (l != null && (bestLocation == null || l.accuracy < bestLocation.accuracy)) {
                        bestLocation = l
                    }
                }
                if (bestLocation != null) {
                    listener.onLocation(bestLocation.latitude, bestLocation.longitude, true)
                } else {
                    listener.onLocation(0.0, 0.0, false)
                }
            }
    }

    /**
     * 停止连续定位
     */
    fun stopContinuousLocation() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
    }

    /**
     * 检查是否有定位权限
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求定位权限
     * 注意：需要在 Activity 里重写 onRequestPermissionsResult
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    }

    /**
     * 检查定位开关
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}