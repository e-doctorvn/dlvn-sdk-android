package com.edoctor.application

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.service.CallService
import com.edoctor.dlvn_sdk.sendbirdCall.VideoCallActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var tvStatus: TextView
    private lateinit var btnLoginUser1: Button
    private lateinit var btnLoginUser2: Button
    private lateinit var btnLogout: Button
    private lateinit var btnVideoCall: Button
    private lateinit var btnVoiceCall: Button
    private lateinit var btnCheckCallStatus: Button
    private lateinit var btnOpenChat: Button
    private lateinit var btnCheckChatStatus: Button
    private lateinit var btnOpenWebView: Button
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    
    private var edoctorDlvnSdk: EdoctorDlvnSdk? = null
    
    // Test credentials (replace with your own)
    private val testUsers = listOf(
        TestUser("dev_user1", "your_access_token_1", "User 1"),
        TestUser("dev_user2", "your_access_token_2", "User 2")
    )
    
    data class TestUser(val userId: String, val accessToken: String, val displayName: String)
    
    companion object {
        private const val TAG = "SendbirdDemo"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initSdk()
        setupClickListeners()
        checkPermissions()
    }
    
    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnLoginUser1 = findViewById(R.id.btn_login_Manh)
        btnLoginUser2 = findViewById(R.id.btn_login_Danh)
        btnLogout = findViewById(R.id.btn_dangxuat)
        btnVideoCall = findViewById(R.id.call_manh)
        btnVoiceCall = findViewById(R.id.call_danh)
        btnCheckCallStatus = findViewById(R.id.btn_check_call_status)
        btnOpenChat = findViewById(R.id.btn_open_chat)
        btnCheckChatStatus = findViewById(R.id.btn_check_chat_status)
        btnOpenWebView = findViewById(R.id.btn_id)
        btnStartService = findViewById(R.id.btn_test_foreground)
        btnStopService = findViewById(R.id.btn_stop_foreground)
    }
    
    private fun initSdk() {
        edoctorDlvnSdk = EdoctorDlvnSdk(this, intent)
        
        // SDK callback when login is requested from WebView
        edoctorDlvnSdk?.onSdkRequestLogin = { url ->
            Log.d(TAG, "SDK requested login, URL: $url")
            showToast("SDK requested login")
        }
        
        updateStatus()
    }
    
    private fun setupClickListeners() {
        // ===== AUTHENTICATION =====
        btnLoginUser1.setOnClickListener {
            showLoginDialog()
        }
        
        btnLoginUser2.setOnClickListener {
            showLoginDialog()
        }
        
        btnLogout.setOnClickListener {
            logout()
        }
        
        // ===== SENDBIRD CALL =====
        btnVideoCall.setOnClickListener {
            startVideoCall()
        }
        
        btnVoiceCall.setOnClickListener {
            startVoiceCall()
        }
        
        btnCheckCallStatus.setOnClickListener {
            checkCallStatus()
        }
        
        // ===== SENDBIRD CHAT =====
        btnOpenChat.setOnClickListener {
            openChat()
        }
        
        btnCheckChatStatus.setOnClickListener {
            checkChatStatus()
        }
        
        // ===== WEBVIEW =====
        btnOpenWebView.setOnClickListener {
            openHealthConsultant()
        }
        
        // ===== SERVICE TEST =====
        btnStartService.setOnClickListener {
            testForegroundService()
        }
        
        btnStopService.setOnClickListener {
            stopForegroundService()
        }
    }
    
    // ==================== AUTHENTICATION ====================
    
    private fun showLoginDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Login to Sendbird")
        builder.setMessage("Enter your Sendbird credentials or use test data")
        
        builder.setPositiveButton("Use Test Data") { _, _ ->
            // Use sample data for testing
            val params = JSONObject().apply {
                put("partnerid", "45f63H33771b42f1b08b7f9a50e6bd8a")
                put("deviceid", "3e030eb9-63e6-4be1-ae0e-940f6b7e2c61")
                put("dcid", "19E2ADB7-91A8-4C32-821B-31A03AD32C89")
                put("token", "26f63593771b42f1b08b7f9a50e6dc7c")
            }
            
            if (edoctorDlvnSdk?.DLVNSendData(params) == true) {
                showToast("✅ Authenticated with test data")
                updateStatus()
            } else {
                showToast("❌ Authentication failed")
            }
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    
    private fun logout() {
        edoctorDlvnSdk?.deauthenticateEDR()
        showToast("🚪 Logged out")
        updateStatus()
    }
    
    // ==================== SENDBIRD CALL ====================
    
    private fun startVideoCall() {
        // Show dialog to enter callee ID
        val builder = AlertDialog.Builder(this)
        builder.setTitle("📹 Start Video Call")
        builder.setMessage("Video call requires authentication via SDK.\nPlease login first using the WebView.")
        
        builder.setPositiveButton("Open WebView") { _, _ ->
            edoctorDlvnSdk?.openWebView(supportFragmentManager, null)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    
    private fun startVoiceCall() {
        showToast("📱 Voice call - Open WebView to start call")
        edoctorDlvnSdk?.openWebView(supportFragmentManager, null)
    }
    
    private fun checkCallStatus() {
        val statusMessage = buildString {
            appendLine("📊 Sendbird Call Status")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Call functionality is managed by SDK")
            appendLine("Open WebView to initiate calls")
            appendLine("")
            appendLine("Features:")
            appendLine("• Video calls with doctors")
            appendLine("• Incoming call notifications")
            appendLine("• Call accept/decline")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Call Status")
            .setMessage(statusMessage)
            .setPositiveButton("OK", null)
            .show()
    }
    
    // ==================== SENDBIRD CHAT ====================
    
    private fun openChat() {
        // Open WebView with chat
        edoctorDlvnSdk?.openWebView(supportFragmentManager, null)
    }
    
    private fun checkChatStatus() {
        val statusMessage = buildString {
            appendLine("💬 Sendbird Chat Status")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Chat functionality is managed by SDK")
            appendLine("Open WebView to access chat")
            appendLine("")
            appendLine("Features:")
            appendLine("• Real-time messaging")
            appendLine("• Push notifications")
            appendLine("• File sharing")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Chat Status")
            .setMessage(statusMessage)
            .setPositiveButton("OK", null)
            .show()
    }
    
    // ==================== WEBVIEW ====================
    
    private fun openHealthConsultant() {
        edoctorDlvnSdk?.openWebView(supportFragmentManager, null)
    }
    
    // ==================== SERVICE TEST ====================
    
    private fun testForegroundService() {
        try {
            Log.d(TAG, "Starting foreground service test...")
            
            val intent = Intent(this, CallService::class.java).apply {
                putExtra("is_heads_up_notification", true)
                putExtra("remote_nickname_or_user_id", "Test User")
                putExtra("call_state", VideoCallActivity.STATE.STATE_ACCEPTING)
                putExtra("call_id", "test_call_${System.currentTimeMillis()}")
                putExtra("is_video_call", true)
                putExtra("do_accept", true)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            showToast("✅ Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            showToast("❌ Error: ${e.message}")
        }
    }
    
    private fun stopForegroundService() {
        try {
            CallService.stopService(this)
            showToast("✅ Service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
            showToast("❌ Error: ${e.message}")
        }
    }
    
    // ==================== HELPERS ====================
    
    private fun updateStatus() {
        // Status is managed by SDK internally
        tvStatus.text = "SDK Ready - Use buttons to interact"
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { 
                it == PackageManager.PERMISSION_GRANTED 
            }
            showToast(if (allGranted) "✅ All permissions granted" else "⚠️ Some permissions denied")
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}