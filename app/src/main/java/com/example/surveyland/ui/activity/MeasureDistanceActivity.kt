package com.example.surveyland.ui.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.R
import com.example.surveyland.databinding.ActivityMeasureDistanceBinding
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.util.StringUtils
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfMeasurement
import kotlin.compareTo

class MeasureDistanceActivity: BaseActivity() {


    private lateinit var mActivityMeasureDistanceBinding: ActivityMeasureDistanceBinding
    private lateinit var mapboxMap: MapboxMap
    // 临时测距点
    private val measurePoints = mutableListOf<Point>()

    // 已完成测距线
    private val completedLines = mutableListOf<List<Point>>()
    private var pointManager: PointAnnotationManager? = null
    private var lineManager: PolylineAnnotationManager? = null
    private var textManager: PointAnnotationManager? = null

    private var totalDistance = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityMeasureDistanceBinding = ActivityMeasureDistanceBinding.inflate(layoutInflater)
        setContentView(mActivityMeasureDistanceBinding.root)

        initMap()
        initClick()
    }

    private fun initClick() {
        mActivityMeasureDistanceBinding.btnFollow.setOnClickListener {
            if (measurePoints.size >= 2) {
                // 保存当前测距到已完成集合
                completedLines.add(measurePoints.toList())
            }
            totalDistance = 0.0
            // 清空当前测距
            measurePoints.clear()

            // 更新 GeoJsonSource，显示原来的线
            updateMeasureLine()
        }
    }
    private fun updateMeasureLine() {
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>("measure-source")?.apply {

                // 所有已完成线 + 当前测距线
                val allLines = mutableListOf<LineString>()

                // 已完成测距
                completedLines.forEach { linePoints ->
                    allLines.add(LineString.fromLngLats(linePoints))
                }

                // 当前测距
                if (measurePoints.size >= 2) {
                    allLines.add(LineString.fromLngLats(measurePoints))
                }

                // 如果只有一条线，直接传入 LineString
                // 如果有多条线，使用 FeatureCollection
                if (allLines.size == 1) {
                    geometry(allLines[0])
                } else {
                    val features = allLines.map { Feature.fromGeometry(it) }
                    featureCollection(FeatureCollection.fromFeatures(features))
                }
            }
//                style.getSourceAs<GeoJsonSource>("measure-source")?.apply {
//                    geometry(LineString.fromLngLats(measurePoints))
//                }
        }
    }
    private fun initMap() {
        mapboxMap = mActivityMeasureDistanceBinding.mapView.getMapboxMap()

        mapboxMap.loadStyleJson(StringUtils.getTdtStyleJson()) { style ->
            val annotationApi = mActivityMeasureDistanceBinding.mapView.annotations
            pointManager = annotationApi.createPointAnnotationManager()
            lineManager = annotationApi.createPolylineAnnotationManager()
            textManager = annotationApi.createPointAnnotationManager()
            initStyle(style)
            mActivityMeasureDistanceBinding.btnStart.setOnClickListener {
                addMeasurePoint()
            }

        }
        //去掉logo
        mActivityMeasureDistanceBinding.mapView.logo.updateSettings {
            enabled = false
        }
        //去掉 Attribution
        mActivityMeasureDistanceBinding.mapView.attribution.updateSettings {
            enabled = false
        }
        //去掉比例尺
        mActivityMeasureDistanceBinding.mapView.scalebar.updateSettings {
            enabled = false
        }

        //显示当前位置
        loadDangqian()
    }
    private fun addMeasurePoint() {

        var point = mapboxMap.cameraState.center

        // 不允许重复添加同一点（经纬度相同或非常接近）
        if (isDuplicatePoint(point)) {
            AppToast.show(this,"不能重复添加相同位置的点")
            return
        }
        // 添加点
         pointManager?.create(
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(BitmapFactory.decodeResource(
                    resources,
                    R.drawable.dian   // 你自己的图标
                ))
                .withDraggable(true)
        )
        measurePoints.add(point)

        // 如果大于1个点，画线 + 计算距离
        if (measurePoints.size > 1) {

            val previous = measurePoints[measurePoints.size - 2]

            updateMeasureLine()

            // 计算距离（单位：公里）
            val distanceKm = TurfMeasurement.distance(previous, point)
            val distanceMeter = distanceKm * 1000

            totalDistance += distanceMeter


            mActivityMeasureDistanceBinding.measures.text = "总长度："+String.format("%.2f m", totalDistance)


            // 计算中点
            val midPoint = getMidPoint(previous, point)
            // 显示当前段距离
            showDistanceLabel(midPoint, String.format("%.2f m", distanceMeter))
        }
    }
    private fun isDuplicatePoint(newPoint: Point): Boolean {
        val thresholdMeters = 1.0 // 认为 1 米以内的点算重复
        for (p in measurePoints) {
            val dist = TurfMeasurement.distance(p, newPoint) * 1000  // km -> m
            if (dist < thresholdMeters) return true
        }
        return false
    }
    private fun getMidPoint(p1: Point, p2: Point): Point {

        val midLng = (p1.longitude() + p2.longitude()) / 2
        val midLat = (p1.latitude() + p2.latitude()) / 2

        return Point.fromLngLat(midLng, midLat)
    }
    // 1️⃣ 创建 Bitmap 绘制文字
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.LEFT
    }
    private fun showDistanceLabel(point: Point, text: String) {

        val width = (paint.measureText(text) + 40).toInt()   // 文字宽 + padding
        val height = 120

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 居中绘制文字
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, width / 2f, height / 2f, paint)

        // 5️⃣ 添加文字 Annotation
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap)

        // 创建新的文字 Annotation
        val annotation = pointManager?.create(pointAnnotationOptions)
    }
    private fun initStyle(style: Style) {
        style.addSource(
            geoJsonSource("measure-source") {
                featureCollection(FeatureCollection.fromFeatures(arrayListOf()))
            }
        )

        style.addLayer(
            lineLayer("measure-layer", "measure-source") {
                lineColor(Color.YELLOW)
                lineWidth(4.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
        )

    }

    private fun loadDangqian() {
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(intent.getDoubleExtra("longitude",0.0), intent.getDoubleExtra("latitude",0.0)))
                .zoom(15.0)
                .build()
        )
    }
}


