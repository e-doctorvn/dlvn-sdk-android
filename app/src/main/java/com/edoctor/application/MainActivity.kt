package com.edoctor.application

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.service.CallService
import com.edoctor.dlvn_sdk.sendbirdCall.VideoCallActivity
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private var myBtn: Button? = null
    private var callManh: Button? = null
    private var callDanh: Button? = null
    private var loginManh: Button? = null
    private var loginDanh: Button? = null
    private var btn_dangxuat: Button? = null
    private var btnTestForeground: Button? = null
    private var btnStopForeground: Button? = null
    private var txtName: TextView? = null
    private var edoctorDlvnSdk: EdoctorDlvnSdk? = null
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edoctorDlvnSdk = EdoctorDlvnSdk(this@MainActivity, intent)

        myBtn = findViewById(R.id.btn_id)
        callManh = findViewById(R.id.call_manh)
        callDanh = findViewById(R.id.call_danh)
        loginManh = findViewById(R.id.btn_login_Manh)
        loginDanh = findViewById(R.id.btn_login_Danh)
        txtName = findViewById(R.id.textView)
        btn_dangxuat = findViewById(R.id.btn_dangxuat)
        btnTestForeground = findViewById(R.id.btn_test_foreground)
        btnStopForeground = findViewById(R.id.btn_stop_foreground)

        edoctorDlvnSdk!!.onSdkRequestLogin = {
            Log.d("zzz", "Nhan duoc data ne")
            Log.d("zzz", it)
        }
        
        // Check and request permissions
        checkPermissions()

        loginManh!!.setOnClickListener {
            txtName!!.text = "Danh (EDR)"
            showToast()
//            edoctorDlvnSdk!!.authenticateSb(
//                this@MainActivity, "dev_danh2", "3b98b5e8b0c2c560d651d02ce1551b6a69cf76ca"
////                "dev_xUqOcSQLXGsxR1i1",
////                "42bc707a751935047ec58391a6da05c42cc6deab"
////                "dev_manh", // dev_0XJyZqTJN7ecrUOc
////                "45fda7a0a7920752243d302738c8be4dabba92b8" // 206d35ef4bf4bed04672e4254db6e06db812b3ec
//            )
        }

        callDanh!!.setOnClickListener {
//            edoctorDlvnSdk!!.sampleFunc("zzz")
//            SendbirdCallImpl.startCall(this@MainActivity, "dev_manh2")
//            edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
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

            if (edoctorDlvnSdk!!.DLVNSendData(params)) {
                edoctorDlvnSdk!!.openWebView(supportFragmentManager, null)
            }
        }

        btn_dangxuat!!.setOnClickListener {
            edoctorDlvnSdk!!.deauthenticateEDR()
        }
        
        // Test Foreground Service Button
        btnTestForeground!!.setOnClickListener {
            testForegroundService()
        }
        
        // Stop Foreground Service Button
        btnStopForeground!!.setOnClickListener {
            stopForegroundService()
        }
    }

    private fun showToast() {
        val inflater = layoutInflater
        val layout: View = inflater.inflate(
            R.layout.toast,
            null
        )

        val text = layout.findViewById<View>(R.id.text_toast) as TextView
        text.text = "Hello! This is a custom toast!"

        val toast = Toast(applicationContext)
        toast.setGravity(Gravity.BOTTOM, 0, 300)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    private fun testForegroundService() {
        try {
            Log.d("ForegroundTest", "Starting foreground service test...")
            Toast.makeText(this, "Testing Foreground Service (remoteMessaging type)", Toast.LENGTH_LONG).show()
            
            // Simulate a test call using CallService from SDK
            val intent = Intent(this, CallService::class.java).apply {
                putExtra("EXTRA_IS_HEADS_UP_NOTIFICATION", true)
                putExtra("EXTRA_REMOTE_NICKNAME_OR_USER_ID", "Test User")
                putExtra("EXTRA_CALL_STATE", VideoCallActivity.STATE.STATE_ACCEPTING)
                putExtra("EXTRA_CALL_ID", "test_call_${System.currentTimeMillis()}")
                putExtra("EXTRA_IS_VIDEO_CALL", true)
                putExtra("EXTRA_DO_ACCEPT", true)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                Log.d("ForegroundTest", "Foreground service started with remoteMessaging type")
                Toast.makeText(this, "✅ Service started with remoteMessaging type", Toast.LENGTH_SHORT).show()
            } else {
                startService(intent)
                Log.d("ForegroundTest", "Service started (pre-O)")
            }
            
        } catch (e: Exception) {
            Log.e("ForegroundTest", "Error starting foreground service", e)
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopForegroundService() {
        try {
            Log.d("ForegroundTest", "Stopping foreground service...")
            CallService.stopService(this)
            Toast.makeText(this, "✅ Service stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ForegroundTest", "Error stopping service", e)
            Toast.makeText(this, "❌ Error stopping: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "✅ All permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Some permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}