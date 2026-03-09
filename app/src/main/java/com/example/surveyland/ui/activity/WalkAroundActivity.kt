package com.example.surveyland.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.buddha.activity.BaseActivity
import com.example.surveyland.R
import com.example.surveyland.dao.DaoUtils
import com.example.surveyland.databinding.ActivityWalkAroundBinding
import com.example.surveyland.ui.view.AppToast
import com.example.surveyland.ui.view.CustomPromptDialog
import com.example.surveyland.util.MapboxLandTracker
import com.example.surveyland.util.StringUtils
import com.example.surveyland.net.repository.TianDiTuRepository
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
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
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WalkAroundActivity: BaseActivity() {

    private lateinit var mActivityWalkAroundBinding: ActivityWalkAroundBinding
    private lateinit var mapboxMap: MapboxMap
    private lateinit var locationManager: LocationManager
    private var isMeasuring = false
    private var isPaused = false
    private var isFollowing = true
    private var isStart = true
    private var lineAnnotationManager: PolylineAnnotationManager? = null
    private var startPointManager: PointAnnotationManager? = null
    private var closeLineManager: PolylineAnnotationManager? = null
    private lateinit var tts: TextToSpeech

    private var startPoint: Point? = null

    // 初始化走一圈管理工具类
    val tracker = MapboxLandTracker.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivityWalkAroundBinding = ActivityWalkAroundBinding.inflate(layoutInflater)
        setContentView(mActivityWalkAroundBinding.root)

        initMap()
        initClick()
        initVoice()
    }

    private fun initVoice() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.CHINA) // 中文
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "语言不支持")
                }
            } else {
                Log.e("TTS", "TTS 初始化失败")
            }
        }
    }
    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "start_measure")
        }
    }
    private fun initClick() {
        mActivityWalkAroundBinding.btnStart.setOnClickListener {
             if (!isStart) {
                 if(tracker.isValidLand()){
                     CustomPromptDialog.Builder(this)
                         .setTitle("提示")
                         .setMessage("确认结束测量么？")
                         .setCancel("取消")
                         .setConfirm("确认") {
                             finishMeasure()
                             mActivityWalkAroundBinding.btnStart.text  = "开始"
                             isStart = !isStart
                         }
                         .show()
                 }else{
                     AppToast.error(this, "未形成有效地块")
                 }
            } else {
                 CustomPromptDialog.Builder(this)
                     .setTitle("提示")
                     .setMessage("为了测量的准确性请保持当前屏幕常亮，由于不同地区信号强弱的问题会有≧1亩的误差，敬请谅解")
                     .setConfirm("知道了") {
                         speak("开始测量")
                         startMeasure()
                         mActivityWalkAroundBinding.btnStart.text = "结束"
                         isStart = !isStart
                     }
                     .show()
            }
        }
        mActivityWalkAroundBinding.btnPause.setOnClickListener { pauseMeasure() }
        mActivityWalkAroundBinding.btnFinish.setOnClickListener {  }
        mActivityWalkAroundBinding.btnFollow.setOnClickListener {
            isFollowing = !isFollowing
            mActivityWalkAroundBinding.btnFollow.text = if (isFollowing) "跟随" else "停止跟随"
        }
    }
    private fun pauseMeasure() {
        isPaused = true
    }

    private fun initMap() {
        mapboxMap = mActivityWalkAroundBinding.mapView.getMapboxMap()

        mapboxMap.loadStyleJson(StringUtils.getTdtStyleJson()) { style ->

            initStyle(style)
            setupAnnotations()

        }
        //去掉logo
        mActivityWalkAroundBinding.mapView.logo.updateSettings {
            enabled = false
        }
        //去掉 Attribution
        mActivityWalkAroundBinding.mapView.attribution.updateSettings {
            enabled = false
        }
        //去掉比例尺
        mActivityWalkAroundBinding.mapView.scalebar.updateSettings {
            enabled = false
        }

        //显示当前位置
        loadDangqian()
    }

    // 缓冲点
    val tempBuffer = mutableListOf<Point>()
    private fun enableLocationComponent() {
        val locationComponentPlugin = mActivityWalkAroundBinding.mapView.location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }
        locationComponentPlugin.updateSettings {
            enabled = true          // 仍然启用定位功能
            pulsingEnabled = false  // 去掉蓝点脉冲效果
            locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@WalkAroundActivity,
                    R.drawable.cancel_account_is
                )
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
            // 初始化起始点
//            if (startPoint == null) {
//                startPoint = point
//                addStartMarker(point)
//            }

            // 每 2 秒记录一次，或者每隔一定距离记录
//            if (tempBuffer.size < 2) return@addOnIndicatorPositionChangedListener
            // 添加轨迹点
            val lng = point.longitude()
            val lat = point.latitude()
            tracker.addPoint(lng, lat)
            drawTrack()
            updateTrack()
            updateArea()
            updateTrackLine()

            mActivityWalkAroundBinding.tvDistance.text = "距离: %.2f m".format(tracker.getPerimeter())

            tempBuffer.clear()
        }
    }

    fun drawTrack() {

        if (tracker.getPoints().size < 2) return
        // 虚线
        mapboxMap.getStyle { style ->
            style.getSourceAs<GeoJsonSource>("dash-source")?.apply {
                //虚线
                if (tracker.getPoints().size >= 3) {
                    geometry(LineString.fromLngLats(listOf(tracker.getPoints().last(), tracker.getPoints().first())))
                }

                if(tracker.isClosed()){
                    geometry(LineString.fromLngLats(emptyList()))
                }
            }
        }
    }

    private fun addStartMarker(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withDraggable(false)
            .withIconImage(BitmapFactory.decodeResource(
                resources,
                R.drawable.dian   // 你自己的图标
            )) // Mapbox 内置图标
        startPointManager?.create(pointAnnotationOptions)
    }

    private fun setupAnnotations() {
        val annotationApi = mActivityWalkAroundBinding.mapView.annotations
        lineAnnotationManager = annotationApi.createPolylineAnnotationManager(mActivityWalkAroundBinding.mapView)
        startPointManager = annotationApi.createPointAnnotationManager(mActivityWalkAroundBinding.mapView)
        closeLineManager = annotationApi.createPolylineAnnotationManager(mActivityWalkAroundBinding.mapView)
    }
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun startMeasure() {

        tracker.reset()

        isMeasuring = true
        isPaused = false

        enableLocationComponent()

        startLocation()

        locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                super.onSatelliteStatusChanged(status)
                var visibleSatellites = 0
                var totalCn0DbHz = 0.0

                for (i in 0 until status.satelliteCount) {
                    val usedInFix = status.usedInFix(i)
                    val cn0 = status.getCn0DbHz(i) // 信号强度
                    if (usedInFix) {
                        visibleSatellites++
                        totalCn0DbHz += cn0
                    }
                }

                val avgSignalStrength =
                    if (visibleSatellites > 0) totalCn0DbHz / visibleSatellites else 0.0

                runOnUiThread {
                    mActivityWalkAroundBinding.tvSatelliteCount.text = "卫星数量: $visibleSatellites"
                    mActivityWalkAroundBinding.tvSignalStrength.text =
                        "平均信号强度: ${"%.1f".format(avgSignalStrength)} dBHz"
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            locationListener
        )

    }
    private fun stopLocation() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

//            if (location.accuracy > 10) return
//
//            // 添加轨迹点
//            val lng = location.longitude
//            val lat = location.latitude
//            tracker.addPoint(lng, lat)
//            drawTrack()
//            updateTrack()
//            updateArea()
//            updateTrackLine()
//
//            mActivityWalkAroundBinding.tvDistance.text = "距离: %.2f m".format(tracker.getPerimeter())
        }
    }

    private fun updateTrackLine() {

        val style = mapboxMap.getStyle() ?: return

        val source = style.getSourceAs<GeoJsonSource>("track-source") ?: return

        if (tracker.getPoints().size < 2) return

        val lineString = LineString.fromLngLats(tracker.getPoints())

        source.feature(
            Feature.fromGeometry(lineString)
        )
    }

    private fun updateArea() {

        if (tracker.getPoints().size < 3) return

        mActivityWalkAroundBinding.tvArea.text = "面积: %.2f ㎡".format(tracker.getLandArea())
    }

    private fun saveToDatabase() {
        //获取当前位置乡镇名称
        val repo = TianDiTuRepository(this)

        repo.getVillageName(tracker.getPoints().get(1).latitude(), tracker.getPoints().get(1).longitude()) { name ->
            lifecycleScope.launch {
                val closedPoints = tracker.getPoints() + tracker.getPoints().first() // 首尾闭合
                //生成缩略图
                val bit = generateSnapshot(mActivityWalkAroundBinding.mapView)

                val file = saveBitmap(bit)

                // 创建 Polygon
                val polygon = Polygon.fromLngLats(listOf(closedPoints))
                val feature = Feature.fromGeometry(polygon)

                // Turf计算中心点
                val centerFeature: Feature = TurfMeasurement.center(feature)
                val centerPoint = centerFeature.geometry() as Point

                DaoUtils.saveLand(
                    this@WalkAroundActivity,
                    name.toString(),
                    tracker.getLandArea(),
                    tracker.getPerimeter(),
                    file.absolutePath,
                    centerPoint.latitude(),
                    centerPoint.longitude(),
                    tracker.getPoints(),
                    1,
                    0,
                    "走一圈"
                )
                dismissLoading()
                finish()
            }
        }

    }

    private suspend fun generateSnapshot(mapView: MapView): Bitmap = withContext(Dispatchers.Main) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            mapView.snapshot { bitmap ->
                if (bitmap != null) {
                    cont.resume(bitmap)  // ✅ 这里是非空 Bitmap
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
    private fun updateTrack() {

        val style = mapboxMap.getStyle() ?: return
        val source = style.getSourceAs<GeoJsonSource>("track-source") ?: return

        if (tracker.getPoints().size < 2) return

        val line = LineString.fromLngLats(tracker.getPoints())
        source.feature(Feature.fromGeometry(line))
    }


    private fun finishMeasure() {

        showLoading("处理中...")
        isMeasuring = false
        stopLocation()

        if (tracker.getPoints().size < 3) return

        val closed = tracker.getPoints().toMutableList()
        closed.add(closed.first())

        saveToDatabase()

    }
    private fun initStyle(style: Style) {
        style.addSource(
            geoJsonSource("track-source") {
                featureCollection(FeatureCollection.fromFeatures(arrayListOf()))
            }
        )

        style.addLayer(
            lineLayer("track-layer", "track-source") {
                lineColor(Color.YELLOW)
                lineWidth(3.0)
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
            }
        )

        style.addSource(
            geoJsonSource("dash-source") {
                geometry(LineString.fromLngLats(emptyList()))
            }
        )

        style.addLayer(
            lineLayer("dash-layer", "dash-source") {
                lineColor(Color.YELLOW)
                lineWidth(3.0)
                lineDasharray(listOf(4.0, 4.0)) //虚线
            }
        )
    }
    private fun loadDangqian() {
        val bounds = CameraBoundsOptions.Builder()
            .minZoom(3.0)  // 最小缩放
            .maxZoom(17.49) // 最大缩放
            .build()
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(intent.getDoubleExtra("longitude",0.0), intent.getDoubleExtra("latitude",0.0)))
                .zoom(intent.getDoubleExtra("zoom",15.0))
                .build()
        )
        mapboxMap.setBounds(bounds)
    }

    override fun onStart() {
        super.onStart()
        mActivityWalkAroundBinding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mActivityWalkAroundBinding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocation()
        mActivityWalkAroundBinding.mapView.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}