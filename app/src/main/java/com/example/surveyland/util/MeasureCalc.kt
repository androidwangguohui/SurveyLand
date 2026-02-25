package com.example.surveyland.util

import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

object MeasureCalc {

    fun area(points: List<Point>): Double {
        if (points.size < 3) return 0.0
        return TurfMeasurement.area(
            Polygon.fromLngLats(listOf(points + points.first()))
        )
    }

    fun perimeter(points: List<Point>): Double {
        var sum = 0.0
        for (i in 0 until points.size - 1) {
            sum += TurfMeasurement.distance(
                points[i],
                points[i + 1],
                TurfConstants.UNIT_METERS
            )
        }
        return sum
    }

    fun edgeLengths(points: List<Point>): List<Double> =
        points.zipWithNext {
                a, b -> TurfMeasurement.distance(a, b, TurfConstants.UNIT_METERS)
        }
}
