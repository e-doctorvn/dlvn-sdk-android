package com.edoctor.dlvn_sdk.service

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.edoctor.dlvn_sdk.R

@SuppressLint("UseCompatLoadingForDrawables")
class AppointmentWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var btnNegative: Button?  = null

    init {
        // Inflate the layout XML file containing your custom view
        LayoutInflater.from(context).inflate(R.layout.home_widget_layout, this, true)

        // Initialize any child views and setup
        // For example:
        // val textView = findViewById<TextView>(R.id.textView)
        btnNegative = findViewById(R.id.btn_negative_wg)
//        btnNegative!!.background = null
//        btnNegative!!.setBackgroundResource(R.drawable.home_widget_button_bg)
    }

    // Add any custom functionality or attributes here
}