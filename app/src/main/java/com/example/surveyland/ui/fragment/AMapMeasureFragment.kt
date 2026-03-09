package com.example.surveyland.ui.fragment


import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.map_amap.util.LocationPermissionViewModel
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.example.surveyland.dao.AppDatabase
import com.example.surveyland.databinding.AmpMeasureFragmentBinding
import com.example.surveyland.entity.LandEntity
import com.example.surveyland.entity.PositionEvent
import com.example.surveyland.ui.activity.MeasureActivity
import com.example.surveyland.ui.activity.MeasureDistanceActivity
import com.example.surveyland.ui.activity.SearchActivity
import com.example.surveyland.ui.activity.VideoActivity
import com.example.surveyland.ui.activity.WalkAroundActivity
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import com.example.surveyland.util.LocationHelper
import com.example.surveyland.util.LocationHelper2
import com.example.surveyland.util.MapClusterHelper
import com.example.surveyland.util.MapboxLocationHelper
import com.example.surveyland.util.StringUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfJoins
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.collections.mutableListOf

class AMapMeasureFragment : BaseFragment() {

    private val permissionVM: LocationPermissionViewModel by activityViewModels()
    private lateinit var polygonManager: PolygonAnnotationManager
    private lateinit var polylineManager: PolylineAnnotationManager
    private lateinit var pointManager2: PointAnnotationManager
    
    private lateinit var pointManager3: PointAnnotationManager
    private val POLYGON_SOURCE = "polygon1-source"
    private val POLYGON_LAYER = "polygon1-layer"
    private val SOLID_SOURCE = "solid1-source"
    private val SOLID_LAYER = "solid1-layer"
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mAmpMeasureFragmentBinding: AmpMeasureFragmentBinding

    private var isSearch: Boolean = false

    private var param1: String? = null

    private var locationClient: AMapLocationClient? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAmpMeasureFragmentBinding = AmpMeasureFragmentBinding.inflate(inflater,container,false)
        polygonManager = mAmpMeasureFragmentBinding.mapView.annotations.createPolygonAnnotationManager()
        polylineManager = mAmpMeasureFragmentBinding.mapView.annotations.createPolylineAnnotationManager()
        pointManager2 = mAmpMeasureFragmentBinding.mapView.annotations.createPointAnnotationManager()
        pointManager3 = mAmpMeasureFragmentBinding.mapView.annotations.createPointAnnotationManager()
        mapboxMap = mAmpMeasureFragmentBinding.mapView.getMapboxMap()

        return mAmpMeasureFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
        EventBus.getDefault().register(this)
        permissionVM.permissionGranted.observe(viewLifecycleOwner) {
            if (it == true) {
                initMap(0)
            }
        }
        initView()
        mAmpMeasureFragmentBinding.tvGo.setOnClickListener {
            requestPermission()
        }
        mAmpMeasureFragmentBinding.location.setOnClickListener {
            isSearch = false
            startLocation()
        }

        mAmpMeasureFragmentBinding.tvMeasure.setOnClickListener {
            val cameraState = mapboxMap.cameraState
            val centerPoint = cameraState.center

            val latitude = centerPoint.latitude()
            val longitude = centerPoint.longitude()
            startActivity(MeasureDistanceActivity::class.java,latitude,longitude,mapboxMap.cameraState.zoom)
        }
    }

    private fun initView() {
        mAmpMeasureFragmentBinding.playVideo.setOnClickListener {
//            startActivity(VideoActivity::class.java,"videoUrl","https://www.w3schools.com/html/mov_bbb.mp4")
            startActivity(VideoActivity::class.java,"videoUrl","http://wu.zmgchub.com/ceshi.mp4")
        }
        mAmpMeasureFragmentBinding.search.setOnClickListener {
            startActivity(SearchActivity::class.java)
        }
        mAmpMeasureFragmentBinding.etSearch.setOnClickListener {
            startActivity(SearchActivity::class.java)
        }
    }



    var isHide: Boolean = false
    private fun setHide() {
        if(mAmpMeasureFragmentBinding.tvHide.text == "显示"){
            isHide = false
            isZoom = false
            val zoom = mapboxMap.cameraState.zoom
            if (zoom < zoomThreshold){
                updateMapDisplay()
            }else{
                loadAllLandFromDatabase(1)
            }
            mAmpMeasureFragmentBinding.tvHide.text = "隐藏"
        }else{
            isHide = true
            clear()
            pointManager3.deleteAll()
            hideLandInfo()
            mAmpMeasureFragmentBinding.tvHide.text = "显示"
        }
    }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startPlotLand(true,1)
        }
    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startPlotLand(true,1)
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPositionEvent(event: PositionEvent) {
        isSearch = true
        lifecycleScope.launch {
            delay(200)   // 延迟200毫秒
            displayMap(event.longitude, event.latitude)
        }
    }
    private fun initMap(type: Int) {

        mapboxMap.loadStyleJson(StringUtils.getTdtStyleJson()) { style ->
            //缩放监听，处理地块显示逻辑
            mapboxMap.addOnCameraChangeListener {
                if(mLandList.isNotEmpty() && !isHide)
                updateMapDisplay()
            }
            mAmpMeasureFragmentBinding.tvHide.setOnClickListener {
                setHide()
            }
            //去掉logo
            mAmpMeasureFragmentBinding.mapView.logo.updateSettings {
                enabled = false
            }
            //去掉 Attribution
            mAmpMeasureFragmentBinding.mapView.attribution.updateSettings {
                enabled = false
            }
            //去掉比例尺
            mAmpMeasureFragmentBinding.mapView.scalebar.updateSettings {
                enabled = false
            }
            initLayers(style)
            //显示当前位置
            if(type == 0)startLocation()
            //点击画地块
            loadDangqian()
            //点击地块
            bindLandClickListener()
        }
    }
    var isZoom : Boolean = false
    val zoomThreshold = 9.0//缩放到多少级进行聚合显示
    private fun updateMapDisplay() {
        val zoom = mapboxMap.cameraState.zoom

        if (zoom < zoomThreshold) {
            if(!isZoom){
                isZoom = true
                // 低缩放 → 聚合显示图标
                hideLandInfo()// 隐藏所有地块线
                clear()
                showClusterIcons()
            }
        } else {
            if(isZoom){
                isZoom = false
                // 高缩放 → 显示地块详情
                pointManager3.deleteAll() // 隐藏聚合图标
                loadAllLandFromDatabase(1)
            }
        }
    }

    fun showClusterIcons() {
        val helper = MapClusterHelper(mapboxMap, pointManager3)
        helper.showClusters(
            requireActivity(),
            landList = mLandList,
            cellSizeMeters = 10000.0,
            zoomWhenClick = 15.0
        )
    }

    private fun bindLandClickListener() {
        //点击
        mAmpMeasureFragmentBinding.mapView.gestures.addOnMapClickListener { point ->
            // 遍历所有地块 Feature

            // 当前点击点
            val clickLng = point.longitude()
            val clickLat = point.latitude()
            val clickPt = Point.fromLngLat(clickLng, clickLat)
            lifecycleScope.launch {
                // 从数据库读取所有地块
                val dao = AppDatabase.getDatabase(requireContext()).landDao()
                val allLands = dao.getAll()
                var clickedLandId: Long? = null

                for (land in allLands) {
                    val points: List<Point> = Gson().fromJson(
                        land.pointsJson,
                        object : TypeToken<List<Point>>() {}.type
                    )

                    val polygon = Polygon.fromLngLats(listOf(points))

                    if (TurfJoins.inside(clickPt, polygon)) {
                        clickedLandId = land.id
                        break
                    }
                }
                // 如果点击到了某个地块
                clickedLandId?.let { id ->
                    Log.e("点击地块ID", "$id")
                    handleLandClick(id)
                }
            }
            true
        }
    }

    private lateinit var locationHelper: MapboxLocationHelper
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocation() {
        if(!isSearch) {
            AMapLocation()
        }
    }

    private fun AMapLocation() {
        locationClient = AMapLocationClient(requireContext())

        val option = AMapLocationClientOption()

        // 高精度模式（GPS + 网络）
        option.locationMode =
            AMapLocationClientOption.AMapLocationMode.Hight_Accuracy

        option.isOnceLocation = true         // 只定位一次
        option.isNeedAddress = false        // 不需要地址更快
        option.isWifiActiveScan = true // 强制开启 wifi 扫描，提高精度
        option.isMockEnable = false
        option.httpTimeOut = 15000
        option.interval = 2000
        option.isLocationCacheEnable = false

        locationClient?.setLocationOption(option)

        locationClient?.setLocationListener { location ->
            if (location != null) {
                if (location.errorCode == 0) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    displayMap(longitude, latitude)
                    Log.d("定位成功", "纬度:$latitude 经度:$longitude")

                } else {
                    AppToast.show(requireActivity(),"高德！！！！高德！！！！")
                    locationHelper = MapboxLocationHelper(requireActivity())

                    locationHelper.getCurrentLocation(
                        onSuccess = { point ->
                            displayMap(point.longitude(), point.latitude())
                            Log.d("LOCATION", "当前经纬度: ${point.latitude()}, ${point.longitude()}")
                        },
                        onError = { error ->
                            AppToast.show(requireActivity(), "mapbox!!!!!!$error")
                            val helper = LocationHelper2(requireContext(), requireActivity())
                            helper.getSingleLocation(object : LocationHelper2.LocationListener {
                                override fun onLocation(latitude: Double, longitude: Double, success: Boolean) {
                                    if (success) {
                                        displayMap(longitude, latitude)
                                    }else{
                                        AppToast.show(requireActivity(),"定位失败，请稍后重试")
                                    }
                                }
                            })
                            Log.w("LOCATION", "获取位置失败: $error")
                        }
                    )

                }
                // 定位完成后停止
                locationClient?.stopLocation()
            }
        }
        locationClient?.startLocation()
    }


    private fun displayMap(lon: Double, lat: Double) {
        val bounds = CameraBoundsOptions.Builder()
            .minZoom(3.0)  // 最小缩放
            .maxZoom(17.49) // 最大缩放
            .build()
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lon, lat))
                .zoom(15.0)
                .build()
        )
        mapboxMap.setBounds(bounds)
    }

    private fun loadDangqian() {
        mAmpMeasureFragmentBinding.tvArea.setOnClickListener {
            startPlotLand(false,1)
        }
    }
    fun startPlotLand(flag: Boolean, type: Int, id: Long?=0) {
        val cameraState = mapboxMap.cameraState
        val centerPoint = cameraState.center

        val latitude = centerPoint.latitude()
        val longitude = centerPoint.longitude()
        startActivity(if(flag) WalkAroundActivity::class.java else MeasureActivity::class.java, "latitude", "longitude", latitude, longitude,type,id,mapboxMap.cameraState.zoom)
    }

    var mLandList :List<LandEntity> = mutableListOf()
    private fun loadAllLandFromDatabase(type: Int) {
        // 1️⃣ 删除所有地块信息
        hideLandInfo()
        if (textAnnotations.isNotEmpty()) {
            // 删除文字
            textAnnotations.forEach { pointManager2.delete(it) }
            // 清空集合
            textAnnotations.clear()
        }
        lifecycleScope.launch {
            mLandList = AppDatabase.getDatabase(requireContext())
                .landDao()
                .getAll()

            if(mLandList.isNotEmpty()){
                withContext(Dispatchers.Main) {
                    showLandOnMap(mLandList)
                }
            }else{
                initMap(type)
            }
        }
    }

    private fun hideLandInfo() {
        mapboxMap.getStyle { style ->
            // 隐藏面
            style.getSourceAs<GeoJsonSource>(POLYGON_SOURCE)
                ?.featureCollection(FeatureCollection.fromFeatures(emptyList()))

            // 隐藏线
            style.getSourceAs<GeoJsonSource>(SOLID_SOURCE)
                ?.featureCollection(FeatureCollection.fromFeatures(emptyList()))
        }
        polygonManager.deleteAll()
        polylineManager.deleteAll()
        pointManager2.deleteAll()
    }

    private val gson = Gson()

    private val allPolygons = mutableListOf<List<Point>>() // 存储所有地块坐标
    private fun showLandOnMap(list: List<LandEntity>) {
        // 清空上次记录
        clear()
        list.forEach { land ->
            val points: List<Point> = gson.fromJson(
                land.pointsJson,
                object : TypeToken<List<Point>>() {}.type
            )
            allPolygons.add(points)
            drawPolygon(points, land.villageName, land.area,land.id)
        }
    }

    private fun clear() {
        allPolygons.clear()
        pointManager2.deleteAll()
        if (textAnnotations.isNotEmpty()) {
            // 删除文字
            textAnnotations.forEach { pointManager2.delete(it) }
            // 清空集合
            textAnnotations.clear()
        }
    }

    // 1️⃣ 创建 Bitmap 绘制文字
    val paint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    private fun drawPolygon(
        points: List<Point>,
        name: String,
        area: Double,
        id: Long,
    ) {
        mapboxMap.getStyle { style ->

            // 1️⃣ 面
            val polygonFeatures = allPolygons.map { points ->
                val closedPoints = points + points.first() // 首尾闭合
                Feature.fromGeometry(Polygon.fromLngLats(listOf(closedPoints))).apply {
                        addNumberProperty("landId", id)
                        addStringProperty("name", name)
                        addNumberProperty("area", area)
                }
            }
            style.getSourceAs<GeoJsonSource>(POLYGON_SOURCE)
                ?.featureCollection(FeatureCollection.fromFeatures(polygonFeatures))

            // 2️⃣ 实线
            val lineFeatures = allPolygons.map { points ->
                val closedPoints = points + points.first()
                Feature.fromGeometry(LineString.fromLngLats(closedPoints))
            }
            style.getSourceAs<GeoJsonSource>(SOLID_SOURCE)
                ?.featureCollection(FeatureCollection.fromFeatures(lineFeatures))
            // 显示文字在面中心
            showAreaText(style, points, paint, mAmpMeasureFragmentBinding.mapView.annotations.createPointAnnotationManager(), area,name)
        }
    }

    private fun handleLandClick(landId: Long) {
        lifecycleScope.launch {
            val land = AppDatabase
                .getDatabase(requireActivity())
                .landDao()
                .getById(landId)
            land?.let {
                showLandDialog(it)
            }
        }
    }
    // 存储所有文字对象
    private val textAnnotations = mutableListOf<PointAnnotation>()
    fun showAreaText(style: Style, points: List<Point>, paint: Paint, pointManager: PointAnnotationManager, area: Double, name: String) {

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
        //%.2f → 浮点数
        //%s → 字符串
        //%d → 整数
//        val text = "%.2f㎡\n%.2f亩".format(area, area / 666.67)
        val text = "%s\n%.2f亩".format(name,area / 666.67)

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

        // 创建文字对象
        val pointAnnotation = pointManager2.create(pointAnnotationOptions)

        // 保存到全局集合
        textAnnotations.add(pointAnnotation)

//        // 先删除之前的文字（防止重复叠加）
//        areaAnnotationId.let { id ->
//            val annotation = pointManager2.annotations.find { it.id == id }
//            if (annotation != null) {
//                pointManager2.delete(annotation)
//            }
//        }
//
//        // 创建新的文字 Annotation
//        val annotation = pointManager2.create(pointAnnotationOptions)
//
//        // 保存 id 方便删除
//        areaAnnotationId = annotation.id
    }


    private fun deleteLand(landId: Long) {
        CustomPromptDialog.Builder(requireActivity())
            .setTitle("提示")
            .setMessage("是否删除该地块？")
            .setCancel("取消")
            .setConfirm("确定") {
                lifecycleScope.launch {
                    val dao = AppDatabase.getDatabase(requireContext()).landDao()
                    val landList = dao.getAll()
                    val targetLand = landList.firstOrNull { it.id == landId }
                    targetLand?.let { dao.delete(it) }
                    //删除后重新刷新地块
                    loadAllLandFromDatabase(1)
                }
            }
            .show()

    }

    override fun onStart() {
        super.onStart()
        mAmpMeasureFragmentBinding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mAmpMeasureFragmentBinding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mAmpMeasureFragmentBinding.mapView.onDestroy()
        locationClient?.onDestroy()
        EventBus.getDefault().unregister(this)

    }

    override fun onLowMemory() {
        super.onLowMemory()
        mAmpMeasureFragmentBinding.mapView.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        loadAllLandFromDatabase(1)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            mAmpMeasureFragmentBinding.mapView.onStop()
        } else {
            mAmpMeasureFragmentBinding.mapView.onStart()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            mAmpMeasureFragmentBinding.mapView.onStart()
        } else {
            mAmpMeasureFragmentBinding.mapView.onStop()
        }
    }
    private fun initLayers(style: Style) {
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
        // 面
        if (style.getSource(POLYGON_SOURCE) == null) {
            style.addSource(geoJsonSource(POLYGON_SOURCE) { geometry(Polygon.fromLngLats(listOf(emptyList()))) })
        }
        if (style.getLayer(POLYGON_LAYER) == null) {
            style.addLayer(fillLayer(POLYGON_LAYER, POLYGON_SOURCE) { fillColor("#388F8E90".toColorInt()) })
        }
    }


    private fun showLandDialog(land: LandEntity) {

        AlertDialog.Builder(requireActivity())
            .setTitle("地块操作")
            .setItems(arrayOf("编辑", "删除")) { _, which ->

                when (which) {
                    0 -> editLand(land)
                    1 -> deleteLand(land.id)
                }
            }
            .show()
    }

    private fun editLand(land: LandEntity) {
        startPlotLand(false,2,land.id)
    }

    companion object {
        private const val ARG_PARAM1 = "param1"

        @JvmStatic
        fun newInstance(param1: String) =
            AMapMeasureFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}






