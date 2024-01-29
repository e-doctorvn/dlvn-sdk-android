package com.edoctor.dlvn_sdk.sendbirdCall

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.helper.PermissionManager
import com.edoctor.dlvn_sdk.service.CallActionReceiver
import com.edoctor.dlvn_sdk.service.CallService
import com.google.android.material.snackbar.Snackbar
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

    private var timeoutValue: Int = 60
    private lateinit var mainHandler: Handler
    private var mReceiver: CallActionReceiver? = null
    private var callManager: CallManager? = CallManager.getInstance()
    private var directCall: DirectCall? = callManager?.directCall
    private val NEEDED_CALLING_PERMISSIONS = "CAMERA_MICROPHONE_NOTIFICATION"

    @RequiresApi(Build.VERSION_CODES.M)
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestAllPermissions() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                Snackbar.make(
                    this.findViewById(android.R.id.content),
                    getString(R.string.request_rational_calling_permissions_msg),
                    Snackbar.LENGTH_INDEFINITE
                ).setTextMaxLines(3).setAction(getString(R.string.incoming_agree_label)) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA,
                            Manifest.permission.POST_NOTIFICATIONS
                        ),
                        PermissionManager.ALL_PERMISSIONS_REQUEST_CODE
                    )
                    PermissionManager.setPermissionAsked(this, NEEDED_CALLING_PERMISSIONS)
                }.show()
            } else {
                if (PermissionManager.getRationalDisplayStatus(this, NEEDED_CALLING_PERMISSIONS)) {
                    Snackbar.make(
                        this.findViewById(android.R.id.content),
                        getString(R.string.request_calling_permissions_msg),
                        Snackbar.LENGTH_INDEFINITE
                    ).setTextMaxLines(3).setAction(getString(R.string.setting_label)) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }.show()
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA,
                            Manifest.permission.POST_NOTIFICATIONS
                        ),
                        PermissionManager.ALL_PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        } else {
            // Permissions already granted
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
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

        callManager?.getAppointmentDetail {
            Glide
                .with(this)
                .load(if (EdoctorDlvnSdk.environment == Constants.Env.SANDBOX) {
                        Constants.edrAttachmentUrlDev
                    } else {
                        Constants.edrAttachmentUrlProd
                    } + it?.doctor?.avatar
                )
                .circleCrop()
                .into(callerAvatar!!)

            txtCaller!!.text = it?.doctor?.degree?.shortName + " " + it?.doctor?.fullName + " gọi video"
        }

        if (directCall?.callee?.nickname != null) {
//            txtCallee!!.text = directCall?.callee?.nickname
        }
//        txtCaller!!.text = "BS. " + directCall?.caller?.nickname + " gọi video"

        mainHandler = Handler(Looper.getMainLooper())
        countdownRingingTimeout()

//        Glide
//            .with(this)
//            .load(directCall?.caller?.profileUrl)
//            .circleCrop()
//            .into(callerAvatar!!)
        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(bgIncoming!!)

//        setUpAnimation()

        acceptCallBtn!!.setOnClickListener {
            if (checkMicCamPermissions()) {
                val context: Context = this@IncomingCallActivity
                val intent = Intent(context, VideoCallActivity::class.java)
                context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

                finish()
            } else {
                var dialog: AlertDialog? = null
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.request_calling_permissions_msg))
                builder.setPositiveButton(getString(R.string.end_time_ok_label)) { _, _ ->
                    dialog?.dismiss()
                    if (
                        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
                    ) {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.POST_NOTIFICATIONS
                            ),
                            PermissionManager.ALL_PERMISSIONS_REQUEST_CODE
                        )
                        PermissionManager.setPermissionAsked(this, NEEDED_CALLING_PERMISSIONS)
                    } else {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }
                }
                builder.setNegativeButton(getString(R.string.btn_close_label)) { _, _ ->
                    dialog?.dismiss()
                }
                dialog = builder.create()
                dialog.show()
            }
        }

        rejectCallBtn!!.setOnClickListener {
            callManager!!.endEclinicCall()
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleCallService() {
        requestAllPermissions()
//        callManager?.handleSendbirdEvent(this@IncomingCallActivity)
        NotificationHelper.cancelCallNotification()
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

    private fun checkMicCamPermissions(): Boolean {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            + ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
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