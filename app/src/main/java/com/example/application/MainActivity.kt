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
//    private val accessToken: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjEsImlhdCI6MTY5MTQwMjY3MSwiZXhwIjoxNjkzMTMwNjcxfQ.o_YSfydvUboC_XjZfm_7pHtk53G0TASgazUL-1Zqh18"

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

        callDanh!!.setOnClickListener {
            edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
        }

        myBtn!!.setOnClickListener {
            val params = JSONObject()
            params.put("partnerid", "45f63H33771b42f1b08b7f9a50e6bd8a")
            params.put("deviceid", "3e030eb9-63e6-4be1-ae0e-940f6b7e2c61")
            params.put("dcid", "19E2ADB7-91A8-4C32-821B-31A03AD32C89")
            params.put("token", "26f63593771b42f1b08b7f9a50e6dc7c")
//            params.put("partnerid", "45f63H33771b42f1b08b7f9a50e6bd8a")
//            params.put("deviceid", "F61A54D7-4276-4D0A-B145-A2320EF15841")
//            params.put("dcid", "FAB02ABB-91E9-47D1-B66C-A8FC4B08335B")
//            params.put("token", "228b15fcd35f4188b393cfbe78378506")

//            if (edoctorDlvnSdk!!.DLVNSendData(params)) {
                edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
//            }
        }

        btn_dangxuat!!.setOnClickListener {
            edoctorDlvnSdk!!.clearWebViewCache()
        }
    }
}