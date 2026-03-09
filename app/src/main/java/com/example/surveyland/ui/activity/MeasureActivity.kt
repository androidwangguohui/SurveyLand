package com.example.surveyland.ui.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.surveyland.R
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.*
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.*
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.dao.AppDatabase
import com.example.surveyland.dao.DaoUtils
import com.example.surveyland.databinding.ActivityMeasureBinding
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import com.example.surveyland.util.MapUtils
import com.example.surveyland.util.StringUtils
import com.example.surveyland.net.repository.TianDiTuRepository
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationDragListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

class MeasureActivity : BaseActivity() {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var zoomMapbox: MapboxMap
    private lateinit var pointManager: PointAnnotationManager

    private lateinit var textManager: PointAnnotationManager
    private lateinit var zoomPointManager: PointAnnotationManager
    private lateinit var zoomLineManager: PolylineAnnotationManager
    private lateinit var lineManager: PolylineAnnotationManager
    private lateinit var polygonManager: PolygonAnnotationManager
    private lateinit var mActivityMeasureBinding: ActivityMeasureBinding

    private val points = mutableListOf<Point>()
    private val pointAnnotations = mutableListOf<com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()
    private val SOLID_SOURCE = "solid-source"
    private val SOLID_LAYER = "solid-layer"

    private val DASH_SOURCE = "dash-source"
    private val DASH_LAYER = "dash-layer"

    private val POLYGON_SOURCE = "polygon-source"
    private val POLYGON_LAYER = "polygon-layer"
    private val ZOOM_LINE_SOURCE = "zoom-solid-line"
    private val ZOOM_LINE_LAYER = "zoom-solid-layer"
    private val ZOOM_DASH_SOURCE = "zoom-dash-line"
    private val ZOOM_DASH_LAYER = "zoom-dash-layer"
    private var areaAnnotationId  :Long = 0

    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityMeasureBinding = ActivityMeasureBinding.inflate(layoutInflater)
        setContentView(mActivityMeasureBinding.root)
        initMap()
        initZoomMap()
        initClick()
        initCompass()
    }

    private fun initClick() {
        mActivityMeasureBinding.btnUp.setOnClickListener {
            rotate3D(pitchDelta = 5.0, bearingDelta = 0.0)
        }
        mActivityMeasureBinding.btnDown.setOnClickListener {
            rotate3D(pitchDelta = -5.0, bearingDelta = 0.0)
        }
        mActivityMeasureBinding.btnLeft.setOnClickListener {
            rotate3D(pitchDelta = 0.0, bearingDelta = -10.0)
        }
        mActivityMeasureBinding.btnRight.setOnClickListener {
            rotate3D(pitchDelta = 0.0, bearingDelta = 10.0)
        }
        mActivityMeasureBinding.btnZoomIn.setOnClickListener {
            zoomMap(1)
        }
        mActivityMeasureBinding.btnZoomOut.setOnClickListener {
            zoomMap(-1)
        }

    }
    private fun rotate3D(pitchDelta: Double, bearingDelta: Double) {

        val state = mapboxMap.cameraState

        var newPitch = state.pitch + pitchDelta
        var newBearing = state.bearing + bearingDelta

        // 限制俯仰范围
        if (newPitch > 60) newPitch = 60.0
        if (newPitch < 0) newPitch = 0.0

        // bearing 归一化
        if (newBearing >= 360) newBearing -= 360
        if (newBearing < 0) newBearing += 360

        mapboxMap.easeTo(
            CameraOptions.Builder()
                .pitch(newPitch)
                .bearing(newBearing)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(300)
            }
        )
    }
    fun zoomMap(type: Int?){
        val zoom = mapboxMap.cameraState.zoom
        if(type == 1){
            mapboxMap.easeTo(
                CameraOptions.Builder()
                    .zoom(zoom + 1)
                    .build()
            )
        }else{
            mapboxMap.easeTo(
                CameraOptions.Builder()
                    .zoom(zoom - 1)
                    .build()
            )
        }
    }
    private fun initInsertPointFeature() {

        mapboxMap.addOnMapClickListener { clickPoint ->

            if (points.size < 2) return@addOnMapClickListener true
            // 不允许重复添加同一点（经纬度相同或非常接近）
            if (isDuplicatePoint(clickPoint)) {
                AppToast.show(this,"不能重复添加相同位置的点")
                return@addOnMapClickListener true
            }
            //判断是否在线上
            val result = MapUtils.findClosestSegment(mapboxMap,points,clickPoint)
            if (result != null) {
                pointAnnotations.clear()
                pointManager.deleteAll()
                lineManager.deleteAll()
                points.add(result.insertIndex, result.projectedPoint)
                drawVertices(points)
            }

            true
        }
    }
    private fun initCompass() {

        val compass = mActivityMeasureBinding.mapView.compass

        // 1️⃣ 开启指南针
        compass.enabled = true

        // 2️⃣ 开启自动隐藏
        compass.fadeWhenFacingNorth = true

        // 3️⃣ 设置位置（四个参数：left, top, right, bottom）
        compass.updateSettings {
            marginLeft = 0f
            marginTop = 200f
            marginRight = 50f
            marginBottom = 0f
        }
    }
    private val gson = Gson()
    private var newName :String = ""
    private fun initEdit() {
        var type = intent.getIntExtra("type",0)
        val editId = intent.getLongExtra("edit_id",0)
        lifecycleScope.launch {
            if(type == 2){
                val land = AppDatabase
                    .getDatabase(this@MeasureActivity)
                    .landDao()
                    .getById(editId)
                land?.let {
                    newName = land.villageName
                    points.clear()
                    val points2: List<Point> = gson.fromJson(
                        land.pointsJson,
                        object : TypeToken<List<Point>>() {}.type
                    )
                    points2.forEach { it
                        addDian(type,it.longitude(),it.latitude())
                    }

                    // 显示文字在面中心
                    showAreaText(null,points, paint, textManager, land.area)
                }
            }
        }
    }

    private fun initZoomMap() {

        zoomMapbox = mActivityMeasureBinding.mapViewZoom.getMapboxMap()
        zoomMapbox.loadStyleJson(StringUtils.getTdtStyleJson()) { style ->

            // 初始化 AnnotationManager
            val annotationApi =  mActivityMeasureBinding.mapViewZoom.annotations
            zoomPointManager = annotationApi.createPointAnnotationManager()
            zoomLineManager = annotationApi.createPolylineAnnotationManager()
            // 初始化空线Source
            if (style.getSource(ZOOM_LINE_SOURCE) == null) {
                style.addSource(geoJsonSource(ZOOM_LINE_SOURCE) { geometry(LineString.fromLngLats(emptyList())) })
            }
            if (style.getLayer(ZOOM_LINE_LAYER) == null) {
                style.addLayer(lineLayer(ZOOM_LINE_LAYER, ZOOM_LINE_SOURCE) {
                    lineColor("#ffffff".toColorInt())
                    lineWidth(4.0)
                })
            }
            // 虚线
            if (style.getSource(ZOOM_DASH_SOURCE) == null) {
                style.addSource(geoJsonSource(ZOOM_DASH_SOURCE) { geometry(LineString.fromLngLats(emptyList())) })
            }
            if (style.getLayer(ZOOM_DASH_LAYER) == null) {
                style.addLayer(lineLayer(ZOOM_DASH_LAYER, ZOOM_DASH_SOURCE) {
                    lineColor("#ffffff".toColorInt())
                    lineWidth(4.0)
                    lineDasharray(listOf(4.0, 2.0))
                })
            }
        }
        //去掉logo
        mActivityMeasureBinding.mapViewZoom.logo.updateSettings {
            enabled = false
        }
        //去掉 Attribution
        mActivityMeasureBinding.mapViewZoom.attribution.updateSettings {
            enabled = false
        }
        //去掉比例尺
        mActivityMeasureBinding.mapViewZoom.scalebar.updateSettings {
            enabled = false
        }
        mActivityMeasureBinding.mapViewZoom.visibility = View.VISIBLE
        mActivityMeasureBinding.mapViewZoom.visibility = View.GONE
    }


    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initMap() {
        mapboxMap = mActivityMeasureBinding.mapView.getMapboxMap()

        mapboxMap.loadStyleJson(StringUtils.getTdtStyleJson()) { style ->
            initLayers(style)
            //如果是点击编辑进入
            initEdit()
            //在这调用打点然后直接删除是解决首次完成后第一个点消失的问题-------------
            addDian()
            delete()
            //-------------------------------------------------------------
            //在线上新增点位
            initInsertPointFeature()
            //点击点位准备拖拽图标
            initMovIcon()
            mActivityMeasureBinding.btnAddPoint.setOnClickListener {
                addDian()
            }
            mActivityMeasureBinding.btnUndo.setOnClickListener {
                undoPoint()
            }
            mActivityMeasureBinding.btnFinish.setOnClickListener {
                CustomPromptDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确认已画好你想圈的地块了么？")
                    .setCancel("取消")
                    .setConfirm("确定") {
                        showLoading("处理中...")
                        // 处理逻辑
                        finishMeasure()
                    }
                    .show()

            }
            mActivityMeasureBinding.btnDelete.setOnClickListener {
                delete()
            }

            pointManager.apply {
                addDragListener(object : OnPointAnnotationDragListener {
                    override fun onAnnotationDragStarted(annotation: Annotation<*>) {
                        val pointAnnotation = annotation as? PointAnnotation ?: return
                        // 拖拽开始，可选高亮
                        mActivityMeasureBinding.mapViewZoom.visibility = View.VISIBLE
                        // 初始化放大位置
                        updateZoomMap(pointAnnotation.point)
                    }
                    override fun onAnnotationDrag(annotation: Annotation<*>) {
                        val pointAnnotation = annotation as? PointAnnotation ?: return
                        // 拖拽过程中实时更新线
                        // 1️⃣ 找到当前拖拽点在列表中的索引
                        val index = pointAnnotation.getData()?.asInt ?: return
                        points[index] = pointAnnotation.point
                        updateLines()
                        // 更新放大 MapView
                        mActivityMeasureBinding.mapViewZoom.visibility = View.VISIBLE
                        updateZoomMap(pointAnnotation.point)
                    }

                    override fun onAnnotationDragFinished(annotation: Annotation<*>) {
                        val pointAnnotation = annotation as? PointAnnotation ?: return
                        // 拖拽结束，最终更新
                        val index = pointAnnotation.getData()?.asInt ?: return
                        points[index] = pointAnnotation.point
                        updateLines()
                        mActivityMeasureBinding.mapViewZoom.visibility = View.GONE
                    }
                })
            }
        }
        //去掉logo
        mActivityMeasureBinding.mapView.logo.updateSettings {
            enabled = false
        }
        //去掉 Attribution
        mActivityMeasureBinding.mapView.attribution.updateSettings {
            enabled = false
        }
        //去掉比例尺
        mActivityMeasureBinding.mapView.scalebar.updateSettings {
            enabled = false
        }

        //显示当前位置
        loadDangqian()
    }

    private fun initMovIcon() {
        pointManager.addClickListener { annotation ->
            //先隐藏所有自定义icon
            hideAllCustomIcons()
            //再更新点击的点位自定义icon
            annotation.iconImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.img_svip_add_three)
            pointManager.update(annotation)

            true
        }
    }

    private fun hideAllCustomIcons() {
        if(pointAnnotations.isNotEmpty()){
            pointAnnotations.forEach { pa ->
                pa.iconImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.dian)
                pointManager.update(pa)
            }
        }
    }

    private fun updateZoomMap(center: Point) {

        // 1️⃣ 更新放大镜 Camera
        mActivityMeasureBinding.mapViewZoom.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(center)
                .zoom(mapboxMap.cameraState.zoom + 2.0) // 放大倍数 2x，可改
                .build()
        )

        // 2️⃣ 同步点
        zoomPointManager.deleteAll()
        points.forEach { pt ->
            zoomPointManager.create(
                PointAnnotationOptions().withPoint(pt)
                    .withDraggable(false)
                    .withIconImage(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.dian   // 你自己的图标
                    ))
            )
        }

        zoomMapbox.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(ZOOM_LINE_SOURCE)?.apply {
                geometry(LineString.fromLngLats(points))
            }
            style.getSourceAs<GeoJsonSource>(ZOOM_DASH_SOURCE)?.apply {
                geometry(LineString.fromLngLats(listOf(points.last(), points.first())))
            }
        }

    }

    //添加点位
    private fun addDian(type:Int? = 0,longitude: Double = 0.0,latitude: Double = 0.0,) {
        val center :Point
        if(type == 0){
            center = mapboxMap.cameraState.center
        }else{
            center = Point.fromLngLat(longitude, latitude)
        }
        // 不允许重复添加同一点（经纬度相同或非常接近）
        if (isDuplicatePoint(center)) {
            AppToast.show(this,"不能重复添加相同位置的点")
            return
        }
        hideAllCustomIcons()
        points.add(center)
        // 添加点
        val annotation = pointManager.create(
            PointAnnotationOptions()
                .withPoint(center)
                .withData(JsonPrimitive(points.indexOf(center)))
                .withIconImage(BitmapFactory.decodeResource(
                    resources,
                    R.drawable.dian   // 你自己的图标
                ))
                .withDraggable(true)
        )
        pointAnnotations.add(annotation)
        //更新点之间的连接线
        updateLines()

    }

    private fun drawVertices(points: List<Point>) {
        points.forEachIndexed { index, point ->
            // 添加点
            val annotation = pointManager.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withData(JsonPrimitive(index))
                    .withIconImage(BitmapFactory.decodeResource(
                        resources,
                        R.drawable.dian   // 你自己的图标
                    ))
                    .withDraggable(true)
            )
            pointAnnotations.add(annotation)
        }
        updateLines()
    }

    private fun isDuplicatePoint(newPoint: Point): Boolean {
        val thresholdMeters = 1.0 // 认为 1 米以内的点算重复
        for (p in points) {
            val dist = TurfMeasurement.distance(p, newPoint) * 1000  // km -> m
            if (dist < thresholdMeters) return true
        }
        return false
    }
    private fun updateLines() {
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(SOLID_SOURCE)?.apply {
                // 实线 ≥2 点
                if (points.size >= 2) {
                    geometry(LineString.fromLngLats(points))
                } else {
                    geometry(LineString.fromLngLats(emptyList()))
                }
            }
            style.getSourceAs<GeoJsonSource>(DASH_SOURCE)?.apply {
                //虚线
                if (points.size >= 3) {
                    mActivityMeasureBinding.btnFinish.visibility = View.VISIBLE
                    geometry(LineString.fromLngLats(listOf(points.last(), points.first())))
                } else {
                    mActivityMeasureBinding.btnFinish.visibility = View.GONE
                    geometry(LineString.fromLngLats(emptyList()))
                }
            }
        }
        //有三个点以上显示面积
        if (points.size >= 3){
            val closedPoints = points + points.first() // 首尾闭合
            // 面积
            val area = TurfMeasurement.area(Polygon.fromLngLats(listOf(closedPoints)))
            // 显示文字在面中心
            showAreaText(null, points, paint, textManager, area)
        }else{
            // 3️⃣ 删除面积文字
            areaAnnotationId.let { id ->
                val annotation = textManager.annotations.find { it.id == id }
                if (annotation != null) {
                    textManager.delete(annotation)
                }
                areaAnnotationId = 0
            }
        }
        if(points.isNotEmpty()){
            mActivityMeasureBinding.tvDistance.text = "周长: %.2f m".format(MapUtils.getPerimeter(points))
        }else{
            mActivityMeasureBinding.tvDistance.text = "周长:0m"
        }
    }
    // 撤回点
    private fun undoPoint() {
        if (points.isEmpty()) return
        hideAllCustomIcons()
        // 删除最后一个点和对应 Annotation
        points.removeLast()
        val lastAnnotation = pointAnnotations.removeLast()
        pointManager.delete(lastAnnotation)

        updateLines()
    }
    private fun delete() {
        // 1️⃣ 清空点数据
        points.clear()
        // 6️⃣ 删除所有打过的点
        pointManager.annotations.forEach { pointManager.delete(it) }
        // 2️⃣ 清空线
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(SOLID_SOURCE)
                ?.geometry(LineString.fromLngLats(emptyList()))

            style.getSourceAs<GeoJsonSource>(DASH_SOURCE)
                ?.geometry(LineString.fromLngLats(emptyList()))

            style.getSourceAs<GeoJsonSource>(POLYGON_SOURCE)
                ?.geometry(Polygon.fromLngLats(listOf(emptyList())))
        }

        // 3️⃣ 删除面积文字
        areaAnnotationId.let { id ->
            val annotation = textManager.annotations.find { it.id == id }
            if (annotation != null) {
                textManager.delete(annotation)
            }
            areaAnnotationId = 0
        }

        // 4️⃣ 隐藏完成按钮
        mActivityMeasureBinding.btnFinish.visibility = View.GONE
    }

    // 1️⃣ 创建 Bitmap 绘制文字
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.LEFT
    }

    // 完成测量
    private fun finishMeasure() {
        mapboxMap.getStyle { style ->

            val closedPoints = points + points.first() // 首尾闭合

            val polygonSource = style.getSourceAs<GeoJsonSource>(POLYGON_SOURCE)

            // 更新面
            polygonSource?.geometry(Polygon.fromLngLats(listOf(closedPoints)))

            // 面积
            val area = TurfMeasurement.area(Polygon.fromLngLats(listOf(closedPoints)))

            // 显示文字在面中心
            showAreaText(style, points, paint, textManager, area)

            //虚线变实线
            mapboxMap.getStyle { style ->
                // 1️⃣ 把完整闭合线给实线 source
                style.getSourceAs<GeoJsonSource>(SOLID_SOURCE)?.apply {
                    geometry(LineString.fromLngLats(closedPoints))
                }

                // 2️⃣ 清空虚线 source
                style.getSourceAs<GeoJsonSource>(DASH_SOURCE)?.apply {
                    geometry(LineString.fromLngLats(emptyList()))
                }
            }
            //获取当前位置乡镇名称
            val repo = TianDiTuRepository(this)

            repo.getVillageName(points.get(1).latitude(), points.get(1).longitude()) { name ->
                lifecycleScope.launch {

                    val type = intent.getIntExtra("type",0)
                    val editId = intent.getLongExtra("edit_id",0)

                    //获取周长
                    val distance = TurfMeasurement.length(LineString.fromLngLats(closedPoints),"meters")
                    //生成缩略图
//                    val bit = generateSnapshot()
                    val bit = MapUtils.generateLandThumbnail(
                        mapView = mActivityMeasureBinding.mapView,
                        mapboxMap = mapboxMap,
                        points = points
                    )
                    val file = saveBitmap(bit)

                    // 创建 Polygon
                    val polygon = Polygon.fromLngLats(listOf(closedPoints))
                    val feature = Feature.fromGeometry(polygon)

                    // Turf计算中心点
                    val centerFeature: Feature = TurfMeasurement.center(feature)
                    val centerPoint = centerFeature.geometry() as Point
                    DaoUtils.saveLand(this@MeasureActivity,
                        if(type == 2)newName else name.toString(),
                        area,distance,
                        file.absolutePath,
                        centerPoint.latitude(),
                        centerPoint.longitude(),
                        points,
                        type,
                        editId,
                        "画地块"
                    )
                    dismissLoading()
                    Log.d("Village", "当前位置乡村：$name")
                    finish()
                }
            }
        }

    }


    private suspend fun generateSnapshot( labelPosition: Point? = null): Bitmap = withContext(Dispatchers.Main){

        // 1️⃣ 计算中心点
        val centerPoint = labelPosition ?: run {
            val polygon = Polygon.fromLngLats(listOf(points + points.first()))
            val feature = Feature.fromGeometry(polygon)
            val centerFeature = TurfMeasurement.center(feature)
            centerFeature.geometry() as Point
        }

        // 2️⃣ 根据面积计算 zoom
        val zoom = MapUtils.calculateZoomByArea(points)

        var cameraOptions = CameraOptions.Builder()
            .center(centerPoint)
            .zoom(zoom)
            .build()

        mapboxMap.setCamera(cameraOptions)

            // 4️⃣ 截图
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                mActivityMeasureBinding.mapView.snapshot { bitmap ->
                    if (bitmap != null) {
                        cont.resume(bitmap)
                    } else {
                        cont.resumeWithException(Exception("Map snapshot failed"))
                    }
                }
            }
    }

    private suspend fun saveBitmap(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "land_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    fun showAreaText(style: Style? = null, points: List<Point>, paint: Paint, pointManager: PointAnnotationManager, area: Double) {

        if (points.size < 3) return  // 至少三点才形成面

        // 1️⃣ 闭合点列表
        val closedPoints = if (points.first() == points.last()) points else points + points.first()

        // 2️⃣ 生成 Polygon
        val polygon = Polygon.fromLngLats(listOf(closedPoints))
        val feature = Feature.fromGeometry(polygon)

        // 3️⃣ Turf.center 获取面中心
        val centerFeature = TurfMeasurement.center(feature)
        val centerPoint = centerFeature.geometry() as Point

        // 4️⃣ 生成文字 Bitmap
//        val text = "%.2f㎡\n%.2f亩".format(area, area / 666.67)
        val text = "%.2f亩".format(area / 666.67)

        val width = (paint.measureText(text) + 40).toInt()   // 文字宽 + padding
        val height = 120

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 居中绘制文字
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, width / 2f, height / 2f, paint)

        // 5️⃣ 添加文字 Annotation
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(centerPoint)
            .withIconImage(bitmap)

        // 先删除之前的文字（防止重复叠加）
        areaAnnotationId?.let { id ->
            val annotation = pointManager.annotations.find { it.id == id }
            if (annotation != null) {
                pointManager.delete(annotation)
            }
        }

        // 创建新的文字 Annotation
        val annotation = pointManager.create(pointAnnotationOptions)

        // 保存 id 方便删除
        areaAnnotationId = annotation.id
    }

    // 初始化图层
    private fun initLayers(style: Style) {
        pointManager = mActivityMeasureBinding.mapView.annotations.createPointAnnotationManager()
        textManager = mActivityMeasureBinding.mapView.annotations.createPointAnnotationManager()
        lineManager = mActivityMeasureBinding.mapView.annotations.createPolylineAnnotationManager()
        polygonManager = mActivityMeasureBinding.mapView.annotations.createPolygonAnnotationManager()
        // 实线
        if (style.getSource(SOLID_SOURCE) == null) {
            style.addSource(geoJsonSource(SOLID_SOURCE) { geometry(LineString.fromLngLats(emptyList())) })
        }
        if (style.getLayer(SOLID_LAYER) == null) {
            style.addLayer(lineLayer(SOLID_LAYER, SOLID_SOURCE) {
                lineColor("#ffffff".toColorInt())
                lineWidth(3.0)
            })
        }

        // 虚线
        if (style.getSource(DASH_SOURCE) == null) {
            style.addSource(geoJsonSource(DASH_SOURCE) { geometry(LineString.fromLngLats(emptyList())) })
        }
        if (style.getLayer(DASH_LAYER) == null) {
            style.addLayer(lineLayer(DASH_LAYER, DASH_SOURCE) {
                lineColor("#ffffff".toColorInt())
                lineWidth(3.0)
                lineDasharray(listOf(2.0, 2.0))
            })
        }

        // 面
        if (style.getSource(POLYGON_SOURCE) == null) {
            style.addSource(geoJsonSource(POLYGON_SOURCE) { geometry(Polygon.fromLngLats(listOf(emptyList()))) })
        }
        if (style.getLayer(POLYGON_LAYER) == null) {
            style.addLayer(fillLayer(POLYGON_LAYER, POLYGON_SOURCE) { fillColor("#388F8E90".toColorInt()) })
        }

    }


    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun loadDangqian() {
        val bounds = CameraBoundsOptions.Builder()
            .minZoom(3.0)  // 最小缩放
            .maxZoom(17.49) // 最大缩放
            .build()
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(intent.getDoubleExtra("longitude",0.0), intent.getDoubleExtra("latitude",0.0)))
                .zoom(intent.getDoubleExtra("zoom",14.0))
                .build()
        )
        mapboxMap.setBounds(bounds)
    }
    // ================= 生命周期 =================

    override fun onStart() {
        super.onStart()
        mActivityMeasureBinding.mapView.onStart()
        mActivityMeasureBinding.mapViewZoom.onStart()
    }

    override fun onStop() {
        super.onStop()
        mActivityMeasureBinding.mapView.onStop()
        mActivityMeasureBinding.mapViewZoom.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivityMeasureBinding. mapView.onDestroy()
        mActivityMeasureBinding. mapViewZoom.onDestroy()
    }
}
