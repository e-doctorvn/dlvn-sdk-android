package com.example.application

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var myBtn: Button? = null
    private var loginManh: Button? = null
    private var btn_dangxuat: Button? = null
    private var inputUserId: EditText? = null
    private var inputAccessToken: EditText? = null
    private var background: LinearLayout? = null
    private var tvUsername: TextView? = null

    private var edoctorDlvnSdk: EdoctorDlvnSdk? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edoctorDlvnSdk = EdoctorDlvnSdk(applicationContext, intent)

        myBtn = findViewById(R.id.btn_id)
        background = findViewById(R.id.background)
        loginManh = findViewById(R.id.btn_login_Manh)
        btn_dangxuat = findViewById(R.id.btn_dangxuat)
        inputUserId = findViewById(R.id.ed_userid)
        tvUsername = findViewById(R.id.tv_username)
        inputAccessToken = findViewById(R.id.ed_access_token)

        background!!.setOnClickListener {
            closeKeyboard()
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

        edoctorDlvnSdk?.onSdkRequestLogin = {
            tvUsername?.text = "User: $it"
        }

        loginManh!!.setOnClickListener {
            if (inputUserId!!.text.toString() != "" && inputAccessToken!!.text.toString() != "") {
                edoctorDlvnSdk!!.authenticateSb(
                    this@MainActivity,
                    inputUserId!!.text.toString(),
                    inputAccessToken!!.text.toString()
                )
            }
        }

        btn_dangxuat!!.setOnClickListener {
            tvUsername?.text = "User: undefined"
            edoctorDlvnSdk!!.clearWebViewCache()
            edoctorDlvnSdk!!.deAuthenticateSb()
        }
    }

    private fun closeKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        } catch (e: Error) {

        }
    }
}