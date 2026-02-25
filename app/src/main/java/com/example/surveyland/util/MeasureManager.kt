package com.example.surveyland.util

import android.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

class MeasureManager(
    private val mapboxMap: MapboxMap,
    private val style: Style
) {

    private val state = MeasureState()
    private val render = MeasureRender(mapboxMap, style)
    private var dragIndex = -1

    fun onMapClick(point: Point) {
        if (state.isGpsMode) return
        state.points.add(point)
        render.update(state.points)
    }

    fun onTouchMove(screenX: Float, screenY: Float) {
        val geo = mapboxMap.coordinateForPixel(
            ScreenCoordinate(screenX.toDouble(), screenY.toDouble())
        )

        if (dragIndex != -1) {
            state.points[dragIndex] =
                Point.fromLngLat(geo.longitude(), geo.latitude())
            render.update(state.points)
        }
    }

    fun onTouchDown(screenX: Float, screenY: Float) {
        val geo = mapboxMap.coordinateForPixel(
            ScreenCoordinate(screenX.toDouble(), screenY.toDouble())
        )

        dragIndex = state.points.indexOfFirst {
            TurfMeasurement.distance(it, geo, TurfConstants.UNIT_METERS) < 5
        }
    }

    fun onTouchUp() {
        dragIndex = -1
    }
}
