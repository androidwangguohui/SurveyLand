package com.example.surveyland.manage

import com.example.surveyland.util.GeoMeasureUtil
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager

class DrawManager(
    private val mapView: MapView
) {
    private val points = mutableListOf<Point>()
    private val annotationApi = mapView.annotations
    private val pointManager = annotationApi.createPointAnnotationManager()
    private val lineManager = annotationApi.createPolylineAnnotationManager()
    private val polygonManager = annotationApi.createPolygonAnnotationManager()

    fun addPoint(point: Point) {
        points.add(point)
        drawPoint(point)
        redraw()
    }

    private fun drawPoint(point: Point) {
        val option = PointAnnotationOptions()
            .withPoint(point)
        pointManager.create(option)
    }

    private fun redraw() {
        lineManager.deleteAll()
        polygonManager.deleteAll()

        if (points.size >= 2) {
            lineManager.create(
                PolylineAnnotationOptions()
                    .withPoints(points)
            )
        }

        if (points.size >= 3) {
            polygonManager.create(
                PolygonAnnotationOptions()
                    .withPoints(listOf(points))
                    .withFillColor("#5533FF33")
            )
        }
    }

    fun getArea(): Double {
        return GeoMeasureUtil.area(points)
    }
}
