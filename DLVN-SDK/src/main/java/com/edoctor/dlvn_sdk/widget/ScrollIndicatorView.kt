package com.edoctor.dlvn_sdk.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ScrollIndicatorView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var indicatorPosition: Float = 0f

    // Update the indicator position
    fun updateIndicatorPosition(position: Float) {
        indicatorPosition = position
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw the indicator line
        canvas.drawLine(indicatorPosition, 0f, indicatorPosition, height.toFloat(), paint)
    }
}