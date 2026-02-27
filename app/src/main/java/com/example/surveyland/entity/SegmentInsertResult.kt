package com.example.surveyland.entity

import com.mapbox.geojson.Point

data class SegmentInsertResult(
    val insertIndex: Int,
    val projectedPoint: Point,
    val distancePx: Double
)
