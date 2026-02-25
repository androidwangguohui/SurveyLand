package com.example.surveyland.util

import com.mapbox.geojson.Point


data class MeasureState(val points: MutableList<Point> = mutableListOf(),
                        var isGpsMode: Boolean = false)
