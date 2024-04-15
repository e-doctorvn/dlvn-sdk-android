package com.edoctor.dlvn_sdk.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.store.AppStore

class AppointmentWidgetList @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var recyclerView: RecyclerView
    private var activeIndicator: ScrollIndicatorView
    private var indicatorHolder: LinearLayout

    init {
        LayoutInflater.from(context).inflate(R.layout.appointment_widget_list_layout, this, true)
        recyclerView = findViewById(R.id.widgetListEdr)
        activeIndicator = findViewById(R.id.scrollIndicatorEdr)
        indicatorHolder = findViewById(R.id.indicatorHolderEdr)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = AppointmentListAdapter()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Calculate the scroll progress
                val scrollOffset = recyclerView.computeHorizontalScrollOffset()
                val maxScrollOffset = recyclerView.computeHorizontalScrollRange() - recyclerView.width
                val percentage = (scrollOffset.toFloat() / maxScrollOffset.toFloat()).coerceIn(0f, 1f)

                // Calculate the translation of the scroll indicator
                val indicatorWidth = activeIndicator.width
                val maxTranslationX = dpToPx(36.toFloat()) - indicatorWidth.toFloat()
                val translationX = maxTranslationX * percentage

                // Update the position of the scroll indicator
                activeIndicator.translationX = translationX
            }
        })

        AppStore.updateWidgetListDisplay = {
            when (it) {
                "LOG_OUT" -> indicatorHolder.visibility = View.GONE
                "HAS_DATA" -> indicatorHolder.visibility = View.VISIBLE
                else -> indicatorHolder.visibility = View.GONE
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
