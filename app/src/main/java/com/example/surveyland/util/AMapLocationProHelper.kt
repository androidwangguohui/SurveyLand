package com.example.surveyland.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.amap.api.location.*

class AMapLocationProHelper(private val context: Context) {

    private var locationClient: AMapLocationClient? = null
    private var callback: ((AMapLocation?) -> Unit)? = null

    private val maxCount = 5
    private var count = 0
    private var bestLocation: AMapLocation? = null

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    /**
     * 启动定位
     * @param maxWaitTime 超时时间(毫秒)，默认 5000
     */
    fun startLocation(maxWaitTime: Long = 5000L, result: (AMapLocation?) -> Unit) {

        callback = result
        count = 0
        bestLocation = null

        locationClient = AMapLocationClient(context)

        val option = AMapLocationClientOption()
        option.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        option.isNeedAddress = true
        option.isOnceLocation = false
        option.isLocationCacheEnable = false
        option.isMockEnable = false
        option.isWifiScan = true
        option.interval = 1000 // 每秒定位一次

        locationClient?.setLocationOption(option)
        locationClient?.setLocationListener(locationListener)

        locationClient?.startLocation()

        // 设置超时
        timeoutRunnable = Runnable {
            Log.w("AMapLocationPro", "定位超时，返回当前最优结果")
            stopLocation()
            callback?.invoke(bestLocation)
        }
        handler.postDelayed(timeoutRunnable!!, maxWaitTime)
    }

    private val locationListener = AMapLocationListener { location ->

        if (location == null) return@AMapLocationListener

        // 定位失败
        if (location.errorCode != 0) {
            count++
            checkFinish()
            return@AMapLocationListener
        }

        // 过滤精度过差或基站定位
        // 精度大于30m 或非GPS(1)定位，忽略
        if (location.accuracy > 30 || location.locationType != 1) {
            count++
            checkFinish()
            return@AMapLocationListener
        }

        // 保存最优 GPS 定位
        if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
            bestLocation = location
        }

        count++
        checkFinish()
    }

    private fun checkFinish() {
        if (count >= maxCount) {
            stopLocation()
            callback?.invoke(bestLocation)
        }
    }

    fun stopLocation() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }
}