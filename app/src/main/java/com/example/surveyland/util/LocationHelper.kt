package com.example.surveyland.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.surveyland.ui.view.AppToast
import com.google.android.gms.location.*

/**
 * LocationHelper 封装类
 *
 * 使用示例：
 *
 * LocationHelper.getInstance(context)
 *     .getCurrentLocation(activity) { lat, lon ->
 *         Log.d("LocationHelper", "lat=$lat, lon=$lon")
 *     }
 */
class LocationHelper private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LocationHelper? = null

        fun getInstance(context: Context): LocationHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * 获取当前位置（一次性获取）
     * @param activity 用于申请权限
     * @param onLocation 回调纬度经度
     */
    fun getCurrentLocation(activity: Activity, onLocation: (lat: Double, lon: Double,status: Boolean) -> Unit) {

        // 1️⃣ 权限检查
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 请求权限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            AppToast.show(activity,"请重新定位")
            Log.w("LocationHelper", "Location permission not granted")
            return
        }

        // 2️⃣ 尝试获取缓存位置
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocation(loc.latitude, loc.longitude,true)
                } else {
                    // 缓存为空，启动实时定位
                    requestLocationUpdates(onLocation, once = true)
                }
            }
            .addOnFailureListener {
                Log.e("LocationHelper", "lastLocation fail: ${it.message}")
                requestLocationUpdates(onLocation, once = true)
            }
    }

    /**
     * 开启持续监听位置
     * @param onLocation 回调纬度经度
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startContinuousLocation(onLocation: (lat: Double, lon: Double,status: Boolean) -> Unit) {
        requestLocationUpdates(onLocation, once = false)
    }

    /**
     * 停止持续监听
     */
    fun stopContinuousLocation() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
    }

    /**
     * 内部方法：请求实时位置
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestLocationUpdates(
        onLocation: (lat: Double, lon: Double,status: Boolean) -> Unit,
        once: Boolean
    ) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 5秒间隔
        ).setMinUpdateIntervalMillis(2000L) // 最小间隔 2秒
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    onLocation(loc.latitude, loc.longitude,true)
                    if (once) {
                        stopContinuousLocation()
                    }
                }else{
                    onLocation(0.0, 0.0,false)
                }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }
}