package com.example.surveyland.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TopMagnifierView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var focusX = 0f
    private var focusY = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val zoom = 2f

    fun updateBitmap(bm: Bitmap, x: Float, y: Float) {
        bitmap = bm
        focusX = x
        focusY = y
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        bitmap?.let { bm ->
            val shader = BitmapShader(bm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()

            matrix.postScale(zoom, zoom)

            val centerY = height / 2f

            matrix.postTranslate(
                -focusX * zoom,
                -focusY * zoom + centerY
            )

            shader.setLocalMatrix(matrix)
            paint.shader = shader

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}