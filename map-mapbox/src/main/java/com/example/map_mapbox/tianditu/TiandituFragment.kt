package com.example.map_mapbox.tianditu

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.map_mapbox.databinding.FragmentMapBinding
import com.example.map_mapbox.ui.MapBoxFragment
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.TileSet
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.RasterSource
import com.mapbox.maps.extension.style.sources.generated.Scheme
import com.mapbox.maps.extension.style.sources.generated.rasterSource

class TiandituFragment : Fragment() {

    private lateinit var mFragmentMapBinding: FragmentMapBinding

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
        initTianditu()
    }
    private fun initTianditu() {
        val mapboxMap = mFragmentMapBinding.mapView.getMapboxMap()

        mapboxMap.loadStyleJson(  """
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

//            style.addLayer(
//                BackgroundLayer("bg").apply {
//                    backgroundColor(Color.BLACK)
//                }
//            )

            // 天地图影像
            style.addSource(
                rasterSource("tdt-img") {
                    tileSize(256)
                    scheme(Scheme.XYZ)
                    tiles(
                        listOf(
                            "https://t0.tianditu.gov.cn/img_w/wmts?" +
                                    "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                                    "&LAYER=img&STYLE=default&TILEMATRIXSET=w" +
                                    "&FORMAT=image/jpeg" +
                                    "&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}" +
                                    "&tk=$tdtKey"
                        )
                    )
                }
            )

            style.addLayer(
                rasterLayer("tdt-img-layer", "tdt-img"){
                    minZoom(1.0)
                    maxZoom(18.0)
                }
            )

            // 天地图中文注记
            style.addSource(
                rasterSource("tdt-cia") {
                    tileSize(256)
                    scheme(Scheme.XYZ)
                    tiles(
                        listOf(
                            "https://t0.tianditu.gov.cn/cia_w/wmts?" +
                                    "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                                    "&LAYER=cia&STYLE=default&TILEMATRIXSET=w" +
                                    "&FORMAT=image/png" +
                                    "&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}" +
                                    "&tk=$tdtKey"
                        )
                    )
                }
            )

            style.addLayerAbove(
                rasterLayer("tdt-cia-layer", "tdt-cia"){
                    minZoom(3.0)
                    maxZoom(18.0)
                },
                "tdt-img-layer"
            )

            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(104.0, 35.0))
                    .zoom(17.0)
                    .build()
            )
        }

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
            TiandituFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}