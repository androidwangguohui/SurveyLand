package com.example.surveyland.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.android.core.location.*
import com.mapbox.geojson.Point

/**
 * Mapbox 高精度定位工具类
 * 获取到有效位置后自动停止实时定位
 */
class MapboxLocationHelper(private val activity: Activity) {

    private var locationEngine: LocationEngine? = null
    private var realtimeCallback: LocationEngineCallback<LocationEngineResult>? = null

    /**
     * 获取设备当前位置
     * @param onSuccess 返回有效经纬度 Point
     * @param onError 获取失败回调
     */
    fun getCurrentLocation(
        onSuccess: (Point) -> Unit,
        onError: (String) -> Unit
    ) {
        // 权限检查
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            onError("没有定位权限")
            return
        }

        // 初始化 LocationEngine
        locationEngine = LocationEngineProvider.getBestLocationEngine(activity)

        // 尝试获取上次已知位置
        locationEngine?.getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                val loc = result?.lastLocation
                if (loc != null && !loc.isFromMockProvider) {
                    // 有效位置直接返回
                    onSuccess(Point.fromLngLat(loc.longitude, loc.latitude))
                    // 停止实时定位（如果之前有注册）
                    stopLocationUpdates()
                } else {
                    // 如果无有效位置，再启动实时更新
                    requestRealtimeLocation(onSuccess, onError)
                }
            }

            override fun onFailure(exception: Exception) {
                // 获取失败也尝试实时更新
                requestRealtimeLocation(onSuccess, onError)
            }
        })
    }

    /**
     * 请求实时位置更新
     */
    private fun requestRealtimeLocation(
        onSuccess: (Point) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = LocationEngineRequest.Builder(3000L) // 3秒间隔
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(5000L) // 最长等待5秒
            .build()

        realtimeCallback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                val loc = result?.lastLocation
                if (loc != null && !loc.isFromMockProvider) {
                    // 获取到有效位置立即回调
                    onSuccess(Point.fromLngLat(loc.longitude, loc.latitude))
                    // 停止实时定位
                    stopLocationUpdates()
                } else {
                    onError("无法获取有效定位")
                }
            }

            override fun onFailure(exception: Exception) {
                onError("实时定位失败: ${exception.message}")
                stopLocationUpdates()
            }
        }

        // 开始实时定位
        locationEngine?.requestLocationUpdates(request,realtimeCallback!!, Looper.getMainLooper())
    }

    /**
     * 停止实时定位
     */
    fun stopLocationUpdates() {
        realtimeCallback?.let {
            locationEngine?.removeLocationUpdates(it)
            realtimeCallback = null
        }
    }
}