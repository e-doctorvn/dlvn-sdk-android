package com.example.application

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dlvn_sdk.EdoctorDlvnSdk

class MainActivity : AppCompatActivity() {
    private var myBtn: Button? = null
    private var callManh: Button? = null
    private var callDanh: Button? = null
    private var loginManh: Button? = null
    private var loginDanh: Button? = null
    private var btn_dangxuat: Button? = null
    private var txtName: TextView? = null
    private var edoctorDlvnSdk: EdoctorDlvnSdk? = null
    private val accessToken: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEsImlhdCI6MTY5MTQwMjY3MSwiZXhwIjoxNjkzMTMwNjcxfQ.o_YSfydvUboC_XjZfm_7pHtk53G0TASgazUL-1Zqh18"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edoctorDlvnSdk = EdoctorDlvnSdk(applicationContext)

        myBtn = findViewById(R.id.btn_id)
        callManh = findViewById(R.id.call_manh)
        callDanh = findViewById(R.id.call_danh)
        loginManh = findViewById(R.id.btn_login_Manh)
        loginDanh = findViewById(R.id.btn_login_Danh)
        txtName = findViewById(R.id.textView)
        btn_dangxuat = findViewById(R.id.btn_dangxuat)

        myBtn!!.setOnClickListener(View.OnClickListener {
            edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
//            dlvnSdk!!.getUserList(mCallback = {
////                Log.d(DLVNSdk.LOG_TAG, "in mainActivity")
//                Log.d(DLVNSdk.LOG_TAG, it.toString())
//            })
        })
    }
}