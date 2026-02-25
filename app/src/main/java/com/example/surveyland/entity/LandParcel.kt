package com.example.surveyland.entity

import com.mapbox.geojson.Point

data class LandParcel( val id: String,
                       val points: List<Point>,
                       val area: Double,
                       val name: String,
                       val createTime: Long)
