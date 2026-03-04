package com.example.surveyland.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.animation.ValueAnimator
import android.graphics.Outline
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.example.surveyland.R

class ThreeLevelSlideView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var startY = 0f
    private var lastY = 0f
    private var currentLevel = 2 // 初始二级
    private var isDragging = false

    private val lineHeight = dp2px(4)
    private val minHeight = dp2px(80) // 一级高度
    private var midHeight = 0 // 二级高度
    private var maxHeight = 0 // 三级高度

    private val animator = ValueAnimator()

    init {
        setBackgroundColor(Color.WHITE)
        post {
            maxHeight = height
            midHeight = (height * 0.5f).toInt()
            setLayerHeight(currentLevel, false)
            // 设置顶部圆角裁剪
            val shapeDrawable = com.google.android.material.shape.MaterialShapeDrawable().apply {
                fillColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
                shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                    .setTopLeftCorner(com.google.android.material.shape.CornerFamily.ROUNDED, dp2px(16).toFloat())
                    .setTopRightCorner(com.google.android.material.shape.CornerFamily.ROUNDED, dp2px(16).toFloat())
                    .setBottomLeftCorner(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
                    .setBottomRightCorner(com.google.android.material.shape.CornerFamily.ROUNDED, 0f)
                    .build()
            }

            background = shapeDrawable
        }

        // 顶部短线
        val topLine = View(context).apply {
//            setBackgroundColor(Color.GRAY)
            layoutParams = LayoutParams(dp2px(40), lineHeight).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dp2px(8)
            }
            background = ContextCompat.getDrawable(context, R.drawable.line_round)
            setOnClickListener {
                toggleLevel()
            }
        }
        addView(topLine)
    }

    private fun dp2px(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.rawY
                lastY = startY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - startY
                if (Math.abs(dy) > dp2px(5)) {
                    isDragging = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - lastY
                lastY = ev.rawY
                moveBy(dy)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                settleLevel()
            }
        }
        return true
    }

    private fun moveBy(dy: Float) {
        val newHeight = height - dy
        val clampedHeight = newHeight.coerceIn(minHeight.toFloat(), maxHeight.toFloat())
        layoutParams.height = clampedHeight.toInt()
        requestLayout()
    }

    private fun settleLevel() {
        val currentHeight = height
        val targetLevel = when {
            currentHeight < (minHeight + midHeight) / 2 -> 1
            currentHeight < (midHeight + maxHeight) / 2 -> 2
            else -> 3
        }
        setLayerHeight(targetLevel, true)
    }

    private fun setLayerHeight(level: Int, animate: Boolean) {
        val targetHeight = when (level) {
            1 -> minHeight
            2 -> midHeight
            3 -> maxHeight
            else -> midHeight
        }

        if (animate) {
            animator.cancel()
            animator.setIntValues(height, targetHeight)
            animator.duration = 200
            animator.addUpdateListener { animation ->
                layoutParams.height = animation.animatedValue as Int
                requestLayout()
            }
            animator.start()
        } else {
            layoutParams.height = targetHeight
            requestLayout()
        }

        currentLevel = level
    }

    private fun toggleLevel() {
        val nextLevel = (currentLevel % 3) + 1
        setLayerHeight(nextLevel, true)
    }
}