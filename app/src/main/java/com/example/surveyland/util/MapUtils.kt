package com.example.surveyland.util

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapSnapshotOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Size
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.mutableListOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
}