package com.example.surveyland.manage

import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.RasterSource
import com.mapbox.maps.extension.style.sources.generated.Scheme
import com.mapbox.maps.extension.style.sources.generated.rasterSource

class MapboxManager(
    private val mapView: MapView
) {
    lateinit var mapboxMap: MapboxMap

    fun init() {
        mapboxMap = mapView.getMapboxMap()

        mapboxMap.loadStyleUri(Style.SATELLITE_STREETS) { style ->
            // 👉 如果你要自定义瓦片（天地图 / 本地）
            addRasterTile()
//            onReady()
        }
    }

    private fun addRasterTile() {
        mapboxMap.loadStyleJson("""
    {
      "version": 8,
      "sources": {},
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": {
            "background-color": "#000000"
          }
        }
      ]
    }
    """.trimIndent()) { style ->

            val tdtKey = "18200bf5ba2f674c772624185d27c1c9"

            style.addSource(
                rasterSource("tdt-img") {
                    tileSize(256)
                    scheme(Scheme.XYZ)
                    tiles(
                        listOf(
                            "https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                                    "&LAYER=img&STYLE=default&TILEMATRIXSET=w" +
                                    "&FORMAT=image/jpeg&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=$tdtKey"
                        )
                    )
                }
            )

            style.addLayer(rasterLayer("tdt-img-layer", "tdt-img") {
                minZoom(1.0)
                maxZoom(18.0)
            })

            style.addSource(
                rasterSource("tdt-cia") {
                    tileSize(256)
                    scheme(Scheme.XYZ)
                    tiles(
                        listOf(
                            "https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                                    "&LAYER=cia&STYLE=default&TILEMATRIXSET=w" +
                                    "&FORMAT=image/png&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=$tdtKey"
                        )
                    )
                }
            )

            style.addLayerAbove(
                rasterLayer("tdt-cia-layer", "tdt-cia") {
                    minZoom(3.0)
                    maxZoom(18.0)
                },
                "tdt-img-layer"
            )

        }
    }
}
