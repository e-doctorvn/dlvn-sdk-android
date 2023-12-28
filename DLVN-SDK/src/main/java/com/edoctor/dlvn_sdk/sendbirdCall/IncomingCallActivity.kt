package com.edoctor.dlvn_sdk.sendbirdCall

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.CallNotificationHelper
import com.edoctor.dlvn_sdk.helper.PermissionManager
import com.edoctor.dlvn_sdk.service.CallActionReceiver
import com.edoctor.dlvn_sdk.service.CallService
import com.sendbird.calls.DirectCall
import jp.wasabeef.glide.transformations.BlurTransformation

class IncomingCallActivity : AppCompatActivity() {
    private var acceptCallBtn: ImageButton? = null
    private var rejectCallBtn: ImageButton? = null
    private var btnToggleMic: ImageButton? = null
    private var btnToggleCam: ImageButton? = null
    private var bgColorIncoming: LinearLayout? = null
    private var txtCaller: TextView? = null
//    private var txtCallee: TextView? = null
//    private var txtTimeout: TextView? = null
    private var bgIncoming: ImageView? = null
    private var callerAvatar: ImageView? = null
    private var mReceiver: CallActionReceiver? = null
    private lateinit var mainHandler: Handler
    private var callManager: CallManager? = CallManager.getInstance()
    private var directCall: DirectCall? = callManager?.directCall
    private var timeoutValue: Int = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        handleCallService()
        initView()
        initCallEventListener()
    }

    // Yêu cầu tất cả các quyền cần thiết
    private fun requestAllPermissions() {
        val missingPermissions = mutableListOf<String>()
        if (!PermissionManager.checkCameraPermission(this)) {
            missingPermissions.add(Manifest.permission.CAMERA)
        }
        if (!PermissionManager.checkMicrophonePermission(this)) {
            missingPermissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!PermissionManager.checkNotificationPermission(this)) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PermissionManager.ALL_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManager.ALL_PERMISSIONS_REQUEST_CODE) {
            // Kiểm tra xem tất cả các quyền đã được cấp hay chưa
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                // Tất cả các quyền đã được cấp, tiến hành thực hiện các tác vụ liên quan đến camera, microphone, và thông báo
                // ở đây
            } else {
                // Nếu người dùng từ chối cấp quyền, bạn có thể hiển thị một thông báo hoặc xử lý một cách phù hợp
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    private fun initView() {
        acceptCallBtn = findViewById(R.id.btn_answer_call)
        rejectCallBtn = findViewById(R.id.btn_reject_call)
        txtCaller = findViewById(R.id.tv_caller_name)
//        txtCallee = findViewById(R.id.tv_user_name)
//        txtTimeout = findViewById(R.id.tv_timeout)
        bgIncoming = findViewById(R.id.bg_incoming)
        callerAvatar = findViewById(R.id.img_caller_avatar)
        btnToggleCam = findViewById(R.id.btn_toggle_cam_incoming)
        btnToggleMic = findViewById(R.id.btn_toggle_mic_incoming)
        bgColorIncoming = findViewById(R.id.bg_color_incoming)

        if (directCall?.callee?.nickname != null) {
//            txtCallee!!.text = directCall?.callee?.nickname
        }
        txtCaller!!.text = "BS. " + directCall?.caller?.nickname + " gọi video"

        mainHandler = Handler(Looper.getMainLooper())
        countdownRingingTimeout()

        Glide
            .with(this)
            .load(directCall?.caller?.profileUrl)
            .circleCrop()
            .into(callerAvatar!!)
        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(bgIncoming!!)

//        setUpAnimation()

        acceptCallBtn!!.setOnClickListener {
            val context: Context = this@IncomingCallActivity
            val intent = Intent(context, VideoCallActivity::class.java)
            context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

            finish()
        }

        rejectCallBtn!!.setOnClickListener {
            callManager!!.expireEclinicRinging()
            callManager!!.directCall?.end()
            CallService.stopService(this)
        }

        btnToggleMic!!.setOnClickListener {
            val value: Boolean = callManager?.acceptCallSetting!!.microphone
            btnToggleMic!!.setImageResource(if (value) R.drawable.ic_mic_ina else R.drawable.ic_mic_atv)
            callManager?.acceptCallSetting!!.microphone = !value
        }
        btnToggleCam!!.setOnClickListener {
            val value: Boolean = callManager?.acceptCallSetting!!.camera
            btnToggleCam!!.setImageResource(if (value) R.drawable.ic_cam_ina else R.drawable.ic_cam_atv)
            callManager?.acceptCallSetting!!.camera = !value
        }
    }

    private fun initCallEventListener() {
        callManager!!.onCallStateChanged = {
            when (it) {
                Constants.CallState.ESTABLISHED -> {}
                Constants.CallState.CONNECTED -> {}
                Constants.CallState.RECONNECTING -> {}
                Constants.CallState.RECONNECTED -> {}
                Constants.CallState.ENDED -> finish()
            }
        }
    }

    private fun handleCallService() {
        requestAllPermissions()
//        callManager?.handleSendbirdEvent(this@IncomingCallActivity)
        CallNotificationHelper.cancelCallNotification()
        CallService.stopService(this)
    }

    private fun removeListenReceiver(context: Context) {
        if (mReceiver != null) {
            context.unregisterReceiver(mReceiver)
            mReceiver = null
        }
    }

    override fun onResume() {
        super.onResume()
        if (
            callManager?.callState == "ENDED"
            && callManager?.pushToken != null
            || callManager?.directCall == null
        ) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeListenReceiver(this)
    }

    private val countdown = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (timeoutValue > 0) {
                timeoutValue -= 1
//                txtTimeout!!.text = "$timeoutValue giây"
            } else {
                mainHandler.removeCallbacks(this)
                finish()
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun countdownRingingTimeout() {
        mainHandler.post(countdown)
    }

    private fun setUpAnimation() {
        val holderAnimation: FrameLayout = findViewById(R.id.avatar_cover)
        val valueAnimator = ValueAnimator.ofFloat(1f, 1.2f)
        valueAnimator.duration = 1500
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.repeatMode = ValueAnimator.REVERSE
        valueAnimator.repeatCount = ValueAnimator.INFINITE

        valueAnimator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            holderAnimation.scaleX = progress
            holderAnimation.scaleY = progress
            holderAnimation.alpha = 1.7f - progress
        }
        valueAnimator.start()
    }
}