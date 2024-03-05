package com.edoctor.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.Constants.CallState
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.DimensionUtils
import com.edoctor.dlvn_sdk.model.Dimension
import com.edoctor.dlvn_sdk.service.CallService
import com.edoctor.dlvn_sdk.store.AppStore
import com.google.android.material.snackbar.Snackbar
import com.sendbird.calls.AcceptParams
import com.sendbird.calls.CallOptions
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdVideoView
import jp.wasabeef.glide.transformations.BlurTransformation
import org.webrtc.RendererCommon
import kotlin.math.abs


class VideoCallActivity : AppCompatActivity() {
    private var localView: SendBirdVideoView? = null
    private var remoteView: SendBirdVideoView? = null
    private var localViewContainer: RelativeLayout? = null
    private var btnOpenChat: ImageButton? = null
    private var btnEndCall: ImageButton? = null
    private var btnRotateCam: ImageButton? = null
    private var btnToggleCam: ImageButton? = null
    private var btnToggleMic: ImageButton? = null
    private var tvMicStatus: TextView? = null
    private var tvCamStatus: TextView? = null
//    private var btnAudioDevices: ImageButton? = null
    private var callManager: CallManager? = null
    private var bgAvatar: ImageView? = null
    private var tvReconnecting: TextView? = null
    private var tvCallTimeout: TextView? = null
    private var bottomOverlay: RelativeLayout? = null
    private var bottomContainer: LinearLayout? = null
    private var chatLoading: ProgressBar? = null
    private var totalCallTime: Int = 1800
    private lateinit var mainHandler: Handler
//    private lateinit var audioDialog: AudioOutputDialog
    private var directCall: DirectCall? = CallManager.getInstance()?.directCall

    enum class STATE {
        STATE_ACCEPTING, STATE_OUTGOING, STATE_CONNECTED, STATE_ENDING, STATE_ENDED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        callManager = CallManager.getInstance()
        initView()
        initCallEventListener()

        CallService.stopService(this)
        callManager?.callState = "CONNECTED"
        callManager?.mContext = this@VideoCallActivity

        val acceptParams = AcceptParams()
        val callOptions = CallOptions()
        callOptions
            .setAudioEnabled(callManager?.acceptCallSetting!!.microphone)
            .setVideoEnabled(callManager?.acceptCallSetting!!.camera)
        localView?.let { callOptions.setLocalVideoView(it) }
        remoteView?.let { callOptions.setRemoteVideoView(it) }
        acceptParams.setCallOptions(callOptions)
        directCall?.accept(acceptParams)
        callManager?.approveEclinicCall()
    }

    override fun onResume() {
        super.onResume()
        initCallEventListener()
        if (chatLoading!!.visibility == View.VISIBLE) {
            chatLoading!!.visibility = View.GONE
            btnOpenChat!!.visibility = View.VISIBLE
        }
        if (callManager!!.callState == "ENDED" && callManager!!.mContext == null) {
            finish()
        } else {
            directCall?.setLocalVideoView(localView)
            directCall?.setRemoteVideoView(remoteView)

            if (!directCall!!.isRemoteVideoEnabled) {
                bgAvatar!!.visibility = View.VISIBLE
            } else {
                bgAvatar!!.visibility = View.INVISIBLE
            }
            toggleLocalView(!directCall!!.isLocalVideoEnabled)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "UseCompatLoadingForDrawables")
    private fun initView() {
        bgAvatar = findViewById(R.id.avatar_as_bg)
        localViewContainer = findViewById(R.id.local_video_container)
        localView = findViewById(R.id.local_video_view)
        remoteView = findViewById(R.id.remote_video_view)
        btnEndCall = findViewById(R.id.btn_end_call)
        btnOpenChat = findViewById(R.id.btn_open_chat)
        btnRotateCam = findViewById(R.id.btn_rotate_cam)
        btnToggleCam = findViewById(R.id.btn_toggle_cam)
        btnToggleMic = findViewById(R.id.btn_toggle_mic)
        tvMicStatus = findViewById(R.id.tv_mic_stt)
        tvCamStatus = findViewById(R.id.tv_cam_stt)
//        btnAudioDevices = findViewById(R.id.btn_audio_devices)
        tvReconnecting = findViewById(R.id.tv_reconnecting)
        tvCallTimeout = findViewById(R.id.tv_call_timeout)
        chatLoading = findViewById(R.id.wv_chat_loading)
        bottomContainer = findViewById(R.id.bottom_call_container)
        bottomOverlay = findViewById(R.id.call_control_overlay)

        val screenSize: Dimension = DimensionUtils.getScreenSize(this)

        val layout = localViewContainer!!.layoutParams
        layout.width = (screenSize.width * 0.27f).toInt()
        layout.height = ((screenSize.width * 0.27f) * 1.5f).toInt()

        remoteView!!.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        remoteView!!.setZOrderMediaOverlay(false)
        remoteView!!.setEnableHardwareScaler(true)
        
        localView!!.setZOrderMediaOverlay(true)
        localView!!.setEnableHardwareScaler(true)

        mainHandler = Handler(Looper.getMainLooper())
        countdownCallTime()
//        audioDialog = AudioOutputDialog(this)

        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(bgAvatar!!)

        directCall?.setLocalVideoView(localView)
        directCall?.setRemoteVideoView(remoteView)

        if (!callManager?.acceptCallSetting!!.camera) {
            toggleLocalView(true)
            btnToggleCam!!.setImageResource(R.drawable.ic_cam_ina)
            tvCamStatus!!.text = getString(R.string.incall_off_cam_label)
        }
        if (!callManager?.acceptCallSetting!!.microphone) {
            btnToggleMic!!.setImageResource(R.drawable.ic_mic_ina)
            tvMicStatus!!.text = getString(R.string.incall_off_mic_label)
        }

        btnEndCall!!.setOnClickListener {
            callManager!!.endEclinicCall()
            directCall?.end()
        }

        btnOpenChat!!.setOnClickListener {
            openWebViewCallActivity()
        }

        btnRotateCam!!.setOnClickListener {
            directCall?.let { it1 -> callManager!!.rotateCamera(it1) }
        }

        btnToggleCam!!.setOnClickListener {
            directCall?.let { it1 -> callManager!!.toggleCam(it1) }
        }

        btnToggleMic!!.setOnClickListener {
            if (callManager!!.directCall?.isLocalAudioEnabled == true) {
                btnToggleMic!!.setImageResource(R.drawable.ic_mic_ina)
                tvMicStatus!!.text = getString(R.string.incall_off_mic_label)
                showOutputActionToast(R.string.incall_off_mic_msg)
            } else {
                btnToggleMic!!.setImageResource(R.drawable.ic_mic_atv)
                tvMicStatus!!.text = getString(R.string.incall_on_mic_label)
                showOutputActionToast(R.string.incall_on_mic_msg)
            }
            directCall?.let { it1 -> callManager!!.toggleMic(it1) }
        }

        AppStore.sdkInstance?.onOpenChatFromNotiInCalling = {
            openWebViewCallActivity()
        }
    }

    private fun openWebViewCallActivity() {
        chatLoading!!.visibility = View.VISIBLE
        btnOpenChat!!.visibility = View.GONE

        val intent = Intent(this, WebViewCallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    @SuppressLint("ShowToast")
    private fun showOutputActionToast(messageId: Int) {
//        val snackbar = Snackbar.make(
//            this.findViewById(android.R.id.content),
//            getString(messageId),
//            Snackbar.LENGTH_SHORT
//        )
//
//        val snackbarLayout: View = snackbar.view
////        val message = snackbarLayout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
////        message.gravity = Gravity.CENTER_HORIZONTAL
////        message.textAlignment = View.TEXT_ALIGNMENT_CENTER
//
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//            FrameLayout.LayoutParams.WRAP_CONTENT
//        )
//        val screenSize: Dimension = DimensionUtils.getScreenSize(this)
//        lp.gravity = Gravity.CENTER_HORIZONTAL
//        lp.setMargins(0, screenSize.height - bottomContainer!!.height - 136, 0, 0)
////        lp.bottomMargin = bottomContainer!!.height + 24
//        // Layout must match parent layout type
//        // Layout must match parent layout type
////        lp.setMargins(0, 0, 0, 0)
//        // Margins relative to the parent view.
//        // This would be 50 from the top left.
//        // Margins relative to the parent view.
//        // This would be 50 from the top left.
//        snackbarLayout.layoutParams = lp
//        snackbar.setTextMaxLines(2)
////        snackbar.show()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
            pictureInPictureParamsBuilder.setAspectRatio(Rational(9, 16))
            pictureInPictureParamsBuilder.setSourceRectHint(Rect(0, 0, 140, 100))
            val pictureInPictureParams = pictureInPictureParamsBuilder.build()
            enterPictureInPictureMode(pictureInPictureParams)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                enterPictureInPictureMode()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,
                                               newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in PiP mode.
            toggleLocalView(true)
            bottomContainer!!.visibility = View.INVISIBLE
            bottomOverlay!!.visibility = View.INVISIBLE
            remoteView!!.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            remoteView!!.requestLayout()
        } else {
            // Restore the full-screen UI.
            toggleLocalView()
            bottomContainer!!.visibility = View.VISIBLE
            bottomOverlay!!.visibility = View.VISIBLE
            remoteView!!.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            remoteView!!.requestLayout()
        }
    }

    private fun initCallEventListener() {
        callManager?.onCallStateChanged = {
            when (it) {
                CallState.ESTABLISHED -> {

                }
                CallState.CONNECTED -> {
                    if (!directCall?.isRemoteVideoEnabled!!) {
                        bgAvatar!!.visibility = View.VISIBLE
                    } else {
                        bgAvatar!!.visibility = View.GONE
                    }
                }
                CallState.RECONNECTING -> {
                    localViewContainer!!.visibility = View.GONE
                    remoteView!!.visibility = View.INVISIBLE
                    bgAvatar!!.visibility = View.VISIBLE
                    tvReconnecting!!.visibility = View.VISIBLE
                }
                CallState.RECONNECTED -> {
                    tvReconnecting!!.visibility = View.GONE
                    toggleCamStatus(directCall?.isLocalVideoEnabled!!)
                    if (!directCall?.isRemoteVideoEnabled!!) {
                        bgAvatar!!.visibility = View.VISIBLE
                        remoteView!!.visibility = View.GONE
                    } else {
                        bgAvatar!!.visibility = View.INVISIBLE
                        remoteView!!.visibility = View.VISIBLE
                    }
                }
                CallState.ENDED -> finish()
            }
        }

        callManager?.onCallActionChanged = {
            when (it) {
                Constants.CallAction.LOCAL_VIDEO -> {
                    toggleCamStatus(directCall?.isLocalVideoEnabled!!)
                }
                else -> {
                    if (!directCall?.isRemoteVideoEnabled!!) {
                        bgAvatar!!.visibility = View.VISIBLE
                    } else {
                        bgAvatar!!.visibility = View.INVISIBLE
                    }
                }
            }
        }

        callManager?.onTotalTimeFetched = {total ->
            totalCallTime = callManager?.appointmentDetail?.callDuration?.let { duration ->
                total.minus(duration).div(1000).let { result -> abs(result) }
            } ?: totalCallTime
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
    }

    private fun toggleLocalView(hide: Boolean = false) {
        if (hide) {
            localView?.visibility = View.GONE
            localViewContainer?.visibility = View.GONE
        } else {
            localView?.visibility = View.VISIBLE
            localViewContainer?.visibility = View.VISIBLE
        }
    }

    private fun toggleCamStatus(on: Boolean) {
        if (!on) {
            toggleLocalView(true)
            btnToggleCam!!.setImageResource(R.drawable.ic_cam_ina)
            tvCamStatus!!.text = getString(R.string.incall_off_cam_label)
            showOutputActionToast(R.string.incall_off_cam_msg)
        } else {
            toggleLocalView()
            btnToggleCam!!.setImageResource(R.drawable.ic_cam_atv)
            tvCamStatus!!.text = getString(R.string.incall_on_cam_label)
            showOutputActionToast(R.string.incall_on_cam_msg)
        }
    }

    private fun formatCallTime(): String {
        if (totalCallTime > 0) {
            val minute = (totalCallTime % 3600) / 60
            val second = (totalCallTime % 60)
            return String.format("00:%02d:%02d", minute, second)
        }
        return "30:00"
    }

    private val countdown = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (totalCallTime > 0) {
                totalCallTime -= 1
                if (totalCallTime == 180) {
                    initEndTimeDialog()
                }
                tvCallTimeout!!.text = formatCallTime()
            } else {
                mainHandler.removeCallbacks(this)
                callManager!!.endEclinicCall()
                directCall!!.end()

            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun countdownCallTime() {
        mainHandler.post(countdown)
    }

    private fun initEndTimeDialog() {
        val li = LayoutInflater.from(this)
        val view: View = li.inflate(R.layout.end_time_dialog, null)

        val alertDialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        alertDialogBuilder.setView(view)

//        val btnCancel = view.findViewById<ImageView>(R.id.btn_end_time_ok)

//        btnCancel.setOnClickListener {
//            // TODO: 7/5/18 your click listener
//        }

        val alertDialogEndTime = alertDialogBuilder.create()
        alertDialogEndTime.show()
        alertDialogEndTime.window?.setLayout(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val closeDialog = Runnable {
            alertDialogEndTime.dismiss()
        }

        Handler(Looper.getMainLooper()).postDelayed(closeDialog, 5000)
    }
}