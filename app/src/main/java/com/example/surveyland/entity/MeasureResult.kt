package com.example.surveyland.entity

import android.graphics.Point

data class MeasureResult(val center: Point,
                         val areaMu: Double,
                         val locationName: String)
