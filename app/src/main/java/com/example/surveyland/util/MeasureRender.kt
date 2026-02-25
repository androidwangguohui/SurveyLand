package com.example.surveyland.util

import android.graphics.Color
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs

class MeasureRender(
    private val mapboxMap: MapboxMap,
    private val style: Style
) {

    init {
        style.addSource(geoJsonSource("m-point"))
        style.addSource(geoJsonSource("m-line"))
        style.addSource(geoJsonSource("m-polygon"))

        style.addLayer(circleLayer("m-point-layer", "m-point") {
            circleRadius(8.0)
            circleColor(Color.RED)
        })

        style.addLayer(lineLayer("m-line-layer", "m-line") {
            lineWidth(2.5)
            lineColor(Color.BLUE)
        })

        style.addLayer(fillLayer("m-polygon-layer", "m-polygon") {
            fillColor(Color.parseColor("#3300FF00"))
        })
    }

    fun update(points: List<Point>) {
        style.getSourceAs<GeoJsonSource>("m-point")
            ?.featureCollection(
                FeatureCollection.fromFeatures(
                    points.map { Feature.fromGeometry(it) }
                )
            )

        if (points.size >= 2) {
            style.getSourceAs<GeoJsonSource>("m-line")
                ?.feature(
                    Feature.fromGeometry(LineString.fromLngLats(points))
                )
        }

        if (points.size >= 3) {
            style.getSourceAs<GeoJsonSource>("m-polygon")
                ?.feature(
                    Feature.fromGeometry(
                        Polygon.fromLngLats(listOf(points + points.first()))
                    )
                )
        }
    }
}
