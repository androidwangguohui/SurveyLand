package com.example.map_amap

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.amap.api.location.*
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polygon
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.*
import com.example.map_amap.databinding.AmpMeasureFragmentBinding
import com.example.map_amap.util.LocationPermissionViewModel
import com.example.map_amap.util.MeasureViewModel

class AMapMeasureFragment : Fragment() {


    private val permissionVM: LocationPermissionViewModel by activityViewModels()
    private val vm: MeasureViewModel by activityViewModels()

    private lateinit var aMap: AMap

    /** 手动打点 */
    private val points = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    /** GPS */
    private lateinit var locationClient: AMapLocationClient
    private val gpsPoints = mutableListOf<LatLng>()
    private lateinit var mAmpMeasureFragmentBinding: AmpMeasureFragmentBinding
    private var param1: String? = null
    // 打点
    private val markers = mutableListOf<Marker>()
    private var polygon: Polygon? = null
    // GPS轨迹
    private var gpsPolyline: Polyline? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mAmpMeasureFragmentBinding = AmpMeasureFragmentBinding.inflate(inflater,container,false)
        // ⚠️ 必须在任何高德 SDK 使用前调用
        AMapLocationClient.updatePrivacyShow(activity, true, true)
        AMapLocationClient.updatePrivacyAgree(activity, true)
        // ✅ 必须先展示隐私政策
        MapsInitializer.updatePrivacyShow(activity, true, true)
        // ✅ 必须同意隐私政策
        MapsInitializer.updatePrivacyAgree(activity, true)
        mAmpMeasureFragmentBinding.mapView.onCreate(savedInstanceState)

        return mAmpMeasureFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
        initMap()
        initClick()
        initDrag()
        initLocation()
        permissionVM.permissionGranted.observe(viewLifecycleOwner) {
            if (it == true) {
                initLocation()  // ⭐ 定位唯一入口
            }
        }

    }

    private fun initMap() {
        aMap = mAmpMeasureFragmentBinding.mapView.map
        aMap.uiSettings.isZoomControlsEnabled = true
        aMap.mapType = AMap.MAP_TYPE_SATELLITE
        aMap.uiSettings.isZoomControlsEnabled = true
        aMap.uiSettings.isMyLocationButtonEnabled = false
        enableMyLocation()
    }
    private fun enableMyLocation() {
        val myLocationStyle = MyLocationStyle()
            .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
            .interval(2000)
            .showMyLocation(true)

        aMap.myLocationStyle = myLocationStyle
        aMap.isMyLocationEnabled = true
        tryMoveCameraToLocation()
    }
    private var lastRealLatLng: LatLng? = null
    private var hasMovedCamera = false
    private fun tryMoveCameraToLocation() {
        if (hasMovedCamera) return
        if (lastRealLatLng == null) return

        hasMovedCamera = true

        aMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                lastRealLatLng!!,
                18f
            )
        )
    }
    private fun initClick() {
        aMap.setOnMapClickListener { latLng ->
            val marker = aMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
            )
            markers.add(marker)
            redrawPolygon()
        }
    }
    private fun redrawPolygon() {
        polygon?.remove()

        if (markers.size < 3) {
            mAmpMeasureFragmentBinding.tvArea.text = "面积：0 ㎡"
            return
        }

        val points = markers.map { it.position }

        polygon = aMap.addPolygon(
            PolygonOptions()
                .addAll(points)
                .strokeWidth(4f)
                .strokeColor(Color.BLUE)
                .fillColor(0x330000FF)
        )

        val area = calculateArea(points)
        mAmpMeasureFragmentBinding.tvArea.text = "面积：%.2f ㎡".format(area)
    }
    private fun calculateArea(points: List<LatLng>): Double {
        var area = 0.0
        val radius = 6378137.0

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            area += Math.toRadians(p2.longitude - p1.longitude) *
                    (2 + Math.sin(Math.toRadians(p1.latitude)) +
                            Math.sin(Math.toRadians(p2.latitude)))
        }

        area = area * radius * radius / 2.0
        return Math.abs(area)
    }

    private fun initDrag() {
        aMap.setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}

            override fun onMarkerDrag(marker: Marker) {
                redrawPolygon()
            }

            override fun onMarkerDragEnd(marker: Marker) {
                redrawPolygon()
            }
        })
    }

    fun clearMeasure() {
        points.clear()
        polygon?.remove()
        polyline?.remove()
        aMap.clear()
    }

    // ================= GPS 轨迹 =================

    private fun initLocation() {
        locationClient = AMapLocationClient(requireActivity())

        val option = AMapLocationClientOption().apply {
            locationMode =
                AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            interval = 2000
            isNeedAddress = false
            isOnceLocationLatest = true
        }

        locationClient.setLocationOption(option)
        locationClient.setLocationListener {
            if (it.errorCode != 0) return@setLocationListener

            val latLng = LatLng(it.latitude, it.longitude)
            lastRealLatLng = latLng
            val ls = ArrayList<Double>()
            ls.add(latLng.latitude)
            ls.add(latLng.longitude)
            vm.longs.value = ls
            if (!hasMovedCamera) {
                hasMovedCamera = true
                aMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                )
            }

            addGpsPoint(latLng)
        }
        locationClient.startLocation()
    }
    private fun addGpsPoint(latLng: LatLng) {
        gpsPoints.add(latLng)

        gpsPolyline?.remove()
        gpsPolyline = aMap.addPolyline(
            PolylineOptions()
                .addAll(gpsPoints)
                .width(6f)
                .color(Color.RED)
        )

        // 如果轨迹闭合，可直接算面积
        if (gpsPoints.size >= 3) {
            val area = calculateArea(gpsPoints)
            mAmpMeasureFragmentBinding.tvArea.text = "轨迹面积：%.2f ㎡".format(area)
        }
    }


    override fun onResume() {
        super.onResume()
        mAmpMeasureFragmentBinding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mAmpMeasureFragmentBinding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
//        handler.removeCallbacksAndMessages(null)
        mAmpMeasureFragmentBinding.mapView.onDestroy()
        if (::locationClient.isInitialized) {
            locationClient.onDestroy()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mAmpMeasureFragmentBinding.mapView.onLowMemory()
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            mAmpMeasureFragmentBinding.mapView.onPause()
        } else {
            mAmpMeasureFragmentBinding.mapView.onResume()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            mAmpMeasureFragmentBinding.mapView.onResume()
        } else {
            mAmpMeasureFragmentBinding.mapView.onPause()
        }
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
