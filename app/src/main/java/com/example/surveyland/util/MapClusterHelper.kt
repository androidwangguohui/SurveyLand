package com.example.surveyland.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.surveyland.R
import com.example.surveyland.entity.LandEntity
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

class MapClusterHelper(
    private val mapboxMap: MapboxMap,
    private val pointAnnotationManager: PointAnnotationManager
) {
    private val gson = Gson()

    /**
     * 显示聚合图标
     * @param landList 地块列表
     * @param cellSizeMeters 每个聚合区域的大小，默认 10000 平米
     * @param zoomWhenClick 聚合点击后缩放到该值
     */
    fun showClusters(
        context: Context,
        landList: List<LandEntity>,
        cellSizeMeters: Double = 10000.0,
        zoomWhenClick: Double = 17.0
    ) {
        // 删除已有聚合图标
        pointAnnotationManager.deleteAll()

        // 生成聚合网格
        val clusters = generateGridClusters(landList, cellSizeMeters)

        // 创建聚合图标
        clusters.forEach { (center, plotsInCell) ->
            val clusterJson: JsonElement = gson.toJsonTree(
                mapOf("plotIds" to plotsInCell.map { it.id }, "plots" to plotsInCell)
            )

            val options = PointAnnotationOptions()
                .withPoint(center)
                .withIconImage(createClusterIcon(plotsInCell.size))
                .withData(clusterJson)

            pointAnnotationManager.create(options)
        }

        // 点击聚合图标
        pointAnnotationManager.addClickListener { clicked ->
            val json = clicked.getData() ?: return@addClickListener true
            // 直接拿出 plotsInCell，不依赖 landList
            val plotsInCell: List<LandEntity> =
                gson.fromJson(json.asJsonObject.get("plots"), object : TypeToken<List<LandEntity>>() {}.type)
            if (plotsInCell.isEmpty()) return@addClickListener true

            // 计算聚合内所有地块的中心
            val avgLng = plotsInCell.map { it.lng }.average()
            val avgLat = plotsInCell.map { it.lat }.average()
            val center = Point.fromLngLat(avgLng, avgLat)
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(center)
                    .zoom(zoomWhenClick)
                    .build()
            )

            // 隐藏聚合图标
            pointAnnotationManager.deleteAll()

            // 展示地块详情
            plotsInCell.forEach { plot -> showPlotDetail(plot) }

            true
        }
    }

    // -------------------- 内部方法 --------------------

    /** 根据地块中心按网格划分聚合区域 */
    private fun generateGridClusters(
        plots: List<LandEntity>,
        cellSizeMeters: Double
    ): List<Pair<Point, List<LandEntity>>> {

        val clusters = mutableListOf<Pair<Point, List<LandEntity>>>()
        if (plots.isEmpty()) return clusters

        val allPoints = plots.map { Point.fromLngLat(it.lng, it.lat) }
        val minLat = allPoints.minOf { it.latitude() }
        val maxLat = allPoints.maxOf { it.latitude() }
        val minLng = allPoints.minOf { it.longitude() }
        val maxLng = allPoints.maxOf { it.longitude() }

        val cellDegree = cellSizeMeters / 111320.0 // 粗略换算

        var lat = minLat
        while (lat <= maxLat) {
            var lng = minLng
            while (lng <= maxLng) {
                val cellPlots = plots.filter { plot ->
                    plot.lat in lat..(lat + cellDegree) &&
                            plot.lng in lng..(lng + cellDegree)
                }

                if (cellPlots.isNotEmpty()) {
                    // 使用区域内地块重心计算聚合图标位置
                    val latCenter = cellPlots.map { it.lat }.average()
                    val lngCenter = cellPlots.map { it.lng }.average()
                    val center = Point.fromLngLat(lngCenter, latCenter)
                    clusters.add(Pair(center, cellPlots))
                }

                lng += cellDegree
            }
            lat += cellDegree
        }

        return clusters
    }

    /** 创建显示地块数量的聚合图标 */
    private fun createClusterIcon(context:Context,count: Int): Bitmap {
        val radius = 60

        // 加载 Drawable
        val drawable = ContextCompat.getDrawable(context, R.drawable.img_svip_love_heart)!!

        // 创建 Bitmap
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        // 绘制 Drawable 到 Canvas
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)


        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(count.toString(), radius.toFloat(), radius.toFloat() + 12f, textPaint)

        return bitmap
    }


    fun createClusterIcon(count: Int): Bitmap {
        val text = "$count"
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 55f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val radius = 60
        val bitmap = Bitmap.createBitmap(radius*2, radius*2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景圆
        val bgPaint = Paint().apply { color = Color.GREEN; isAntiAlias = true }
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), bgPaint)

        // 绘制数量
        canvas.drawText(text, radius.toFloat(), radius.toFloat() + 12f, paint)

        return bitmap
    }

    /** 展示地块详情，可在此绘制 Polygon 或弹窗信息 */
    private fun showPlotDetail(plot: LandEntity) {
        // 缩放到指定值
//        mapboxMap.setCamera(
//            CameraOptions.Builder()
//                .center(Point.fromLngLat(plot.lng, plot.lat))
//                .zoom(14.0) // 目标缩放
//                .build()
//        )
    }
}