package com.jj.zds.feature.micsig.ui

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class OscilloscopeZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val displayMatrix = Matrix()
    private var minimumScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val targetScale = (currentScale() * detector.scaleFactor)
                .coerceIn(minimumScale, minimumScale * MAX_SCALE_MULTIPLIER)
            val factor = targetScale / currentScale()
            displayMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            imageMatrix = displayMatrix
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        post(::resetMatrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                displayMatrix.postTranslate(event.x - lastX, event.y - lastY)
                imageMatrix = displayMatrix
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    private fun resetMatrix() {
        val image = drawable ?: return
        if (width == 0 || height == 0 || image.intrinsicWidth <= 0 || image.intrinsicHeight <= 0) return
        minimumScale = min(
            width.toFloat() / image.intrinsicWidth,
            height.toFloat() / image.intrinsicHeight,
        )
        val dx = (width - image.intrinsicWidth * minimumScale) / 2f
        val dy = (height - image.intrinsicHeight * minimumScale) / 2f
        displayMatrix.setScale(minimumScale, minimumScale)
        displayMatrix.postTranslate(dx, dy)
        imageMatrix = displayMatrix
    }

    private fun currentScale(): Float {
        val values = FloatArray(9)
        displayMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private companion object {
        const val MAX_SCALE_MULTIPLIER = 5f
    }
}
