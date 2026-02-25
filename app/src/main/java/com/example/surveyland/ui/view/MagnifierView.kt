package com.example.surveyland.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.mapbox.geojson.Point

class MagnifierView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintLine = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val paintDashed = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
        isAntiAlias = true
    }

    private val paintPoint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var mapBitmap: Bitmap? = null
    private var scaleFactor = 2f       // 放大倍数
    private var centerX = 0f
    private var centerY = 0f

    private val points = mutableListOf<Point>()
    private val screenPoints = mutableListOf<Pair<Float, Float>>() // 放大镜坐标

    fun setMapBitmap(bitmap: Bitmap, centerScreenX: Float, centerScreenY: Float, scale: Float = 2f) {
        mapBitmap = bitmap
        centerX = centerScreenX
        centerY = centerScreenY
        scaleFactor = scale
        invalidate()
    }

    fun setPoints(list: List<Point>, mapToScreen: (Point) -> Pair<Float, Float>) {
        points.clear()
        screenPoints.clear()
        points.addAll(list)
        points.forEach {
            screenPoints.add(mapToScreen(it))
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mapBitmap?.let { bitmap ->
            val widthF = width.toFloat()
            val heightF = height.toFloat()
            val srcWidth = widthF / scaleFactor
            val srcHeight = heightF / scaleFactor

            val srcLeft = (centerX - srcWidth / 2).coerceIn(0f, bitmap.width - srcWidth)
            val srcTop = (centerY - srcHeight / 2).coerceIn(0f, bitmap.height - srcHeight)

            val src = Rect(
                srcLeft.toInt(),
                srcTop.toInt(),
                (srcLeft + srcWidth).toInt(),
                (srcTop + srcHeight).toInt()
            )
            val dst = Rect(0, 0, width, height)
            canvas.drawBitmap(bitmap, src, dst, null)
        }

        // 绘制线
        for (i in 0 until screenPoints.size - 1) {
            val (x1, y1) = screenPoints[i]
            val (x2, y2) = screenPoints[i + 1]

            val lx1 = (x1 - centerX) * scaleFactor + width / 2
            val ly1 = (y1 - centerY) * scaleFactor + height / 2
            val lx2 = (x2 - centerX) * scaleFactor + width / 2
            val ly2 = (y2 - centerY) * scaleFactor + height / 2

            if (i % 2 == 0) {
                canvas.drawLine(lx1, ly1, lx2, ly2, paintLine)
            } else {
                canvas.drawLine(lx1, ly1, lx2, ly2, paintDashed)
            }
        }

        // 绘制点
        screenPoints.forEach { (x, y) ->
            val px = (x - centerX) * scaleFactor + width / 2
            val py = (y - centerY) * scaleFactor + height / 2
            canvas.drawCircle(px, py, 8f, paintPoint)
        }
    }
}