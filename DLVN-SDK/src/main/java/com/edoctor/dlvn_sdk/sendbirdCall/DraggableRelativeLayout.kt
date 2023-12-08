package com.edoctor.dlvn_sdk.sendbirdCall

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import kotlin.math.abs

class DraggableRelativeLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs), View.OnTouchListener {
    private val CLICK_DRAG_TOLERANCE = 10f
    private var downRawX: Float = 0f
    private var downRawY: Float = 0f
    private var dX: Float = 0f
    private var dY: Float = 0f

    init {
        setOnTouchListener(this)
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        val layoutParams: MarginLayoutParams = view?.layoutParams as MarginLayoutParams

        val action: Int? = motionEvent?.action
        return if (action == MotionEvent.ACTION_DOWN) {
            downRawX = motionEvent.rawX
            downRawY = motionEvent.rawY
            dX = view.x - downRawX
            dY = view.y - downRawY
            true
        } else if (action == MotionEvent.ACTION_MOVE) {
            val margin = 20
            val viewWidth: Int = view.width
            val viewHeight: Int = view.height
            val viewParent = view.parent as View
            val parentWidth = viewParent.width
            val parentHeight = viewParent.height

            var newX: Float = motionEvent.rawX + dX
            newX = margin.toFloat()
                .coerceAtLeast(newX)
            newX = (parentWidth - viewWidth - margin).toFloat()
                .coerceAtMost(newX)
            var newY: Float = motionEvent.rawY + dY
            newY = margin.toFloat()
                .coerceAtLeast(newY)
            newY = (parentHeight - viewHeight - layoutParams.bottomMargin).toFloat()
                .coerceAtMost(newY)
            view.animate()
                .x(newX)
                .y(newY)
                .setDuration(0)
                .start()
            true
        } else if (action == MotionEvent.ACTION_UP) {
            val upRawX: Float = motionEvent.rawX
            val upRawY: Float = motionEvent.rawY
            val upDX = upRawX - downRawX
            val upDY = upRawY - downRawY
            if (abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE) {
                performClick()
            } else {
                true
            }
        } else {
            super.onTouchEvent(motionEvent)
        }
    }
}