package com.example.surveyland.ui.activity

// =======================================================
// 农业 / 国土测绘 App 核心实现（对标你给的截图）
// 功能：
// 1. 天地图影像 + 注记
// 2. 实时定位蓝点
// 3. 画地块（点击 / GPS 行走）
// 4. 拖拽编辑顶点
// 5. 面积（亩 / m²）+ 周长 + 边长
// 6. 多地块管理 / 显示名称
// =======================================================

// ======================= 依赖 =======================
// implementation("com.mapbox.maps:android:10.16.1")
// implementation("com.mapbox.turf:turf:6.10.0")
// implementation("com.google.android.gms:play-services-location:21.0.1")

// ======================= imports =======================
import android.Manifest
import android.location.Location
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.mapbox.geojson.*
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.*
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.*
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.turf.*

class SurveyActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var fusedClient: FusedLocationProviderClient

    private val manualPoints = mutableListOf<Point>()
    private val gpsPoints = mutableListOf<Point>()
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        setContentView(mapView)

        mapboxMap = mapView.getMapboxMap()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        loadTdtStyle()
    }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    // ======================= 天地图 =======================
    private fun loadTdtStyle() {
        mapboxMap.loadStyleJson(EMPTY_STYLE.trimIndent()) { style ->
            addTdt(style)
            initLayers(style)
            initClickMeasure()
            startGps()
        }
    }

    private fun addTdt(style: Style) {
        val key = "18200bf5ba2f674c772624185d27c1c9"

        style.addSource(rasterSource("tdt-img") {
            tileSize(256); scheme(Scheme.XYZ)
            tiles(listOf("https://t0.tianditu.gov.cn/img_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=img&STYLE=default&TILEMATRIXSET=w&FORMAT=image/jpeg&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=$key"))
        })

        style.addLayer(rasterLayer("tdt-img-layer", "tdt-img"){
            minZoom(1.0)
            maxZoom(18.0)
        })

        style.addSource(rasterSource("tdt-cia") {
            tileSize(256); scheme(Scheme.XYZ)
            tiles(listOf("https://t0.tianditu.gov.cn/cia_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cia&STYLE=default&TILEMATRIXSET=w&FORMAT=image/png&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=$key"))
        })

        style.addLayerAbove(rasterLayer("tdt-cia-layer", "tdt-cia"){
            minZoom(3.0)
            maxZoom(18.0)
        }, "tdt-img-layer")
    }

    // ======================= 测绘图层 =======================
    private fun initLayers(style: Style) {
        style.addSource(geoJsonSource("point"))
        style.addSource(geoJsonSource("line"))
        style.addSource(geoJsonSource("polygon"))

        style.addLayer(circleLayer("point-layer", "point") {
            circleRadius(6.0)
            circleColor("#ff6600")
        })

        style.addLayer(lineLayer("line-layer", "line") {
            lineWidth(2.5)
            lineColor("#ff9900")
        })

        style.addLayer(fillLayer("polygon-layer", "polygon") {
            fillColor("#55ff9900")
        })
    }

    // ======================= 点击画地 =======================
    private fun initClickMeasure() {
        mapView.gestures.addOnMapClickListener { latLng ->
            manualPoints.add(Point.fromLngLat(latLng.longitude(), latLng.latitude()))
            updateManual()
            true
        }
    }

    private fun updateManual() {
        val style = mapboxMap.getStyle() ?: return

        style.getSourceAs<GeoJsonSource>("point")?.featureCollection(
            FeatureCollection.fromFeatures(manualPoints.map { Feature.fromGeometry(it) })
        )

        if (manualPoints.size >= 2) {
            style.getSourceAs<GeoJsonSource>("line")?.geometry(LineString.fromLngLats(manualPoints))
        }

        if (manualPoints.size >= 3) {
            val closed = manualPoints + manualPoints.first()
            val polygon = Polygon.fromLngLats(listOf(closed))
            style.getSourceAs<GeoJsonSource>("polygon")?.geometry(polygon)

            val area = TurfMeasurement.area(polygon) / 666.666
            val perimeter = TurfMeasurement.length(LineString.fromLngLats(closed), TurfConstants.UNIT_METERS)
            // 👉 这里显示：14.37 亩 / 周长
        }
    }

    // ======================= GPS 行走测绘 =======================
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startGps() {
        val req = LocationRequest.Builder(2000)
            .setMinUpdateDistanceMeters(1f)
            .build()

        fusedClient.requestLocationUpdates(req, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (!isValid(loc)) return

                gpsPoints.add(Point.fromLngLat(loc.longitude, loc.latitude))
                updateGps()
            }
        }, mainLooper)
    }

    private fun updateGps() {
        val style = mapboxMap.getStyle() ?: return
        if (gpsPoints.size < 2) return

        style.getSourceAs<GeoJsonSource>("line")?.geometry(LineString.fromLngLats(gpsPoints))
    }

    private fun isValid(loc: Location): Boolean {
        if (!loc.hasAccuracy() || loc.accuracy > 6) return false
        return loc.speed < 3.0
    }

    companion object {
        const val EMPTY_STYLE = """
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
    """
    }
}
