package com.example.map_mapbox.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource
import com.example.map_mapbox.databinding.FragmentMapBinding
import com.mapbox.maps.loader.MapboxMapsInitializer

class MapBoxFragment: Fragment() {

    private lateinit var mFragmentMapBinding: FragmentMapBinding
    private lateinit var mapboxMap: MapboxMap

    private var param1: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mFragmentMapBinding = FragmentMapBinding.inflate(inflater,container,false)

        return mFragmentMapBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
        }
        mapboxMap = mFragmentMapBinding.mapView.getMapboxMap()

        // 加载默认样式
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // 在样式加载完成后叠加瓦片
            addCustomRasterTiles(style)
        }
    }

private fun addCustomRasterTiles(style: Style) {

    // -- 1. 创建瓦片源
    val tileUrl = "https://maps.yinongkeji.com/maps/vt/lyrs=s&x=0&y=0&z=0"

    val rasterSource = rasterSource("custom-raster-source") {
        tileSet("tileset", listOf(tileUrl)) {
            tileSize(256)
        }
        // 可选：设置瓦片 min/max zoom
//        minZoom(0)
//        maxZoom(19)
    }

    // 添加到样式
    style.addSource(rasterSource)

    // -- 2. 创建栅格图层
    val rasterLayer = rasterLayer(
        layerId = "custom-raster-layer",
        sourceId = "custom-raster-source"
    ) {
        // 这里可以先留空
    }

    // 添加图层到地图（放在最底层）
    style.addLayer(rasterLayer)

    // -- 3. 初始镜头设置
    mapboxMap.setCamera(
        CameraOptions.Builder()
            .center(com.mapbox.geojson.Point.fromLngLat(116.3913, 39.9075))
            .zoom(12.0)
            .build()
    )
}

    override fun onStart() {
        super.onStart()
        mFragmentMapBinding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mFragmentMapBinding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mFragmentMapBinding.mapView.onDestroy()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"

        @JvmStatic
        fun newInstance(param1: String) =
            MapBoxFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}