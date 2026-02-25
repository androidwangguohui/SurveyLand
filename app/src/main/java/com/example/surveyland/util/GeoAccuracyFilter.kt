package com.example.surveyland.util

import android.location.Location

object GeoAccuracyFilter {

    fun isValid(prev: Location?, curr: Location): Boolean {
        if (curr.accuracy > 5) return false
        if (prev == null) return true

        val speed =
            curr.distanceTo(prev) / (curr.time - prev.time) * 1000

        return speed < 2.5   // 农田 / 步行合理速度
    }
}
