package com.edoctor.dlvn_sdk.helper

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.hardware.display.DisplayManagerCompat
import com.edoctor.dlvn_sdk.model.Dimension

object DimensionUtils {
    fun getScreenSize(context: Context): Dimension {
        var width = 0
        var height = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val defaultDisplay =
                DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext = context.createDisplayContext(defaultDisplay!!)

            width = displayContext.resources.displayMetrics.widthPixels
            height = displayContext.resources.displayMetrics.heightPixels
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)

            height = (displayMetrics.heightPixels / 1)
            width = (displayMetrics.widthPixels / 1)
        }

        return Dimension(width, height)
    }
}