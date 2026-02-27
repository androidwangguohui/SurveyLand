package com.example.surveyland.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.example.surveyland.entity.SegmentInsertResult
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Size
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.mutableListOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

object  MapUtils {

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

    private suspend fun saveBitmap(context: Context,bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "land_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    /** 周长（米） */
    open fun getPerimeter(points:List<Point>): Double {
        val closedPoints = getClosedPoints(points)
        if (closedPoints.size < 2) return 0.0
        var perimeter = 0.0
        for (i in 0 until closedPoints.size - 1) {
            perimeter += TurfMeasurement.distance(closedPoints[i], closedPoints[i + 1], TurfConstants.UNIT_METERS)
        }
        return perimeter
    }
    fun getClosedPoints(points:List<Point>): List<Point> =
        points + points.first()

    /**
     * 根据地块点集合生成 Bitmap 缩略图
     * @param mapView 当前 MapView
     * @param points 地块点集合（至少 3 个）
     * @return Bitmap 地块截图
     */
    suspend fun generateLandThumbnail(
        mapView: MapView,
        mapboxMap: MapboxMap,
        points: List<Point>
    ): Bitmap = suspendCancellableCoroutine { cont ->

        if (points.size < 3) {
            cont.resumeWithException(Exception("Need at least 3 points"))
            return@suspendCancellableCoroutine
        }

        // 1️⃣ 闭合多边形
        val closedPoints = if (points.first() == points.last()) points else points + points.first()

        // 2️⃣ 构建 Polygon 和中心点
        val polygon = Polygon.fromLngLats(listOf(closedPoints))
        val feature = Feature.fromGeometry(polygon)
        val centerFeature = TurfMeasurement.center(feature)
        val centerPoint = centerFeature.geometry() as Point

        // 3️⃣ 根据面积计算基础 zoom
        val area = TurfMeasurement.area(feature)
        val baseZoom = when {
            area < 500 -> 18.0
            area < 2000 -> 17.0
            area < 10000 -> 16.0
            area < 50000 -> 15.0
            area < 200000 -> 14.0
            area < 1000000 -> 13.0
            area < 5000000 -> 12.0
            area < 20000000 -> 11.0
            else -> 10.0
        }

        // 4️⃣ 使用 bounds 自动计算相机（双保险）
        val boundsCamera = mapboxMap.cameraForCoordinates(
            coordinates = closedPoints,
            padding = EdgeInsets(100.0, 100.0, 100.0, 100.0),
            bearing = 0.0,
            pitch = 0.0
        )

        // 5️⃣ 最终 zoom
        val finalZoom = minOf(baseZoom, boundsCamera.zoom ?: baseZoom)

        // 6️⃣ 设置相机
        val camera = CameraOptions.Builder()
            .center(boundsCamera.center)
            .zoom(finalZoom)
            .pitch(0.0)
            .bearing(0.0)
            .build()

        mapboxMap.setCamera(camera)

        // 7️⃣ 延迟一帧，保证地图渲染完成（可选，稳定截图）
        mapView.postDelayed( {
            mapView.snapshot { bitmap ->
                if (bitmap != null) {
                    cont.resume(bitmap)
                } else {
                    cont.resumeWithException(Exception("Map snapshot failed"))
                }
            }
        },500)

        // 8️⃣ 取消协程时取消 MapView snapshot（可选）
        cont.invokeOnCancellation {
            // MapView snapshot 目前没有取消接口，可根据实际需求处理
        }
    }

    open fun calculateZoomByArea(points: List<Point>): Double {

        if (points.size < 3) return 18.0

        // 1️⃣ 闭合多边形
        val closed = if (points.first() == points.last()) {
            points
        } else {
            points + points.first()
        }

        // 2️⃣ 构建 Polygon
        val polygon = Polygon.fromLngLats(listOf(closed))
        val feature = Feature.fromGeometry(polygon)

        // 3️⃣ 计算面积（平方米）
        val area = TurfMeasurement.area(feature)

        // 4️⃣ 根据面积返回 zoom
        return when {
            area < 500 -> 19.0          // 小于 500㎡
            area < 2000 -> 18.0
            area < 10000 -> 17.0
            area < 50000 -> 16.0
            area < 200000 -> 15.0
            area < 1000000 -> 14.0
            area < 5000000 -> 13.0
            area < 20000000 -> 12.0
            else -> 11.0                // 超大地块
        }
    }

    //查找最近线段
    open fun findClosestSegment(mapboxMap:MapboxMap,polygonPoints:List<Point>,clickPoint: Point): SegmentInsertResult? {

        var minDistancePx = Double.MAX_VALUE
        var bestResult: SegmentInsertResult? = null

        val size = polygonPoints.size
        val isClosed = polygonPoints.first() == polygonPoints.last()

        val segmentCount = if (isClosed) size - 1 else size - 1

        for (i in 0 until segmentCount) {

            val start = polygonPoints[i]
            val end = if (i == size - 1) polygonPoints[0] else polygonPoints[i + 1]

            val projected = projectPointOnSegment(clickPoint, start, end)

            val screenClick = mapboxMap.pixelForCoordinate(clickPoint)
            val screenProjected = mapboxMap.pixelForCoordinate(projected)

            val dx = screenClick.x - screenProjected.x
            val dy = screenClick.y - screenProjected.y
            val distancePx = sqrt(dx * dx + dy * dy)

            if (distancePx < 15 && distancePx < minDistancePx) {
                minDistancePx = distancePx
                bestResult = SegmentInsertResult(i + 1, projected, distancePx)
            }
        }

        return bestResult
    }
    //计算点在线段上的投影
    private fun projectPointOnSegment(
        p: Point,
        a: Point,
        b: Point
    ): Point {

        val px = p.longitude()
        val py = p.latitude()
        val ax = a.longitude()
        val ay = a.latitude()
        val bx = b.longitude()
        val by = b.latitude()

        val dx = bx - ax
        val dy = by - ay

        if (dx == 0.0 && dy == 0.0) return a

        val t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0.0, 1.0)

        val projX = ax + clampedT * dx
        val projY = ay + clampedT * dy

        return Point.fromLngLat(projX, projY)
    }
}