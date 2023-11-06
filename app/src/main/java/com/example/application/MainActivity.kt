package com.example.application

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dlvn_sdk.EdoctorDlvnSdk
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var myBtn: Button? = null
    private var callManh: Button? = null
    private var callDanh: Button? = null
    private var loginManh: Button? = null
    private var loginDanh: Button? = null
    private var btn_dangxuat: Button? = null
    private var txtName: TextView? = null
    private var edoctorDlvnSdk: EdoctorDlvnSdk? = null

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

        edoctorDlvnSdk!!.onSdkRequestLogin = {
            Log.d("zzz", "Nhan duoc data ne")
            Log.d("zzz", it)
        }

        myBtn!!.setOnClickListener {
            val params = JSONObject()
            params.put("partnerid", "45f63H33771b42f1b08b7f9a50e6bd8a")
            params.put("deviceid", "3e030eb9-63e6-4be1-ae0e-940f6b7e2c61")
            params.put("dcid", "19E2ADB7-91A8-4C32-821B-31A03AD32C89")
            params.put("token", "26f63593771b42f1b08b7f9a50e6dc7c")

            if (edoctorDlvnSdk!!.DLVNSendData(params)) {
                edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
            }
        }
    }
}