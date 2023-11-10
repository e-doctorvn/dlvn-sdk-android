package com.example.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.app.ActivityOptions
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
import android.view.View
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
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.Constants.CallState
import com.example.dlvn_sdk.R
//import com.example.dlvn_sdk.components.AudioOutputDialog
import com.example.dlvn_sdk.service.CallService
import com.example.dlvn_sdk.store.AppStore
import com.sendbird.calls.AcceptParams
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.CallOptions
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdVideoView
import com.sendbird.calls.handler.DirectCallListener
import jp.wasabeef.glide.transformations.BlurTransformation

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
    private var doctorAvatar: ImageView? = null
    private var callManager: CallManager? = null
    private var bgAvatar: ImageView? = null
    private var tvCalleeName: TextView? = null
    private var tvDoctorName: TextView? = null
    private var tvCallTimeout: TextView? = null
    private var bottomOverlay: RelativeLayout? = null
    private var bottomContainer: LinearLayout? = null
    private var chatLoading: ProgressBar? = null
    private var totalCallTime: Int = 600
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
        callManager = AppStore.callManager
        initView()
        initCallListener()

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
    }

    override fun onResume() {
        super.onResume()
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

            initCallListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
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
        doctorAvatar = findViewById(R.id.img_doctor_avatar)
        tvCalleeName = findViewById(R.id.tv_callee_name)
        tvDoctorName = findViewById(R.id.tv_doctor_name)
        tvCallTimeout = findViewById(R.id.tv_call_timeout)
        chatLoading = findViewById(R.id.wv_chat_loading)
        bottomContainer = findViewById(R.id.bottom_call_container)
        bottomOverlay = findViewById(R.id.call_control_overlay)

        mainHandler = Handler(Looper.getMainLooper())
        countdownCallTime()
//        audioDialog = AudioOutputDialog(this)

//        Glide
//            .with(this)
//            .load(directCall?.caller?.profileUrl)
//            .centerCrop()
//            .into(doctorAvatar!!)
//        Glide
//            .with(this)
//            .load(directCall?.caller?.profileUrl)
//            .centerCrop()
//            .into(bgAvatar!!)
        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(bgAvatar!!)

        tvDoctorName!!.text = directCall?.caller?.nickname
        tvCalleeName!!.text = directCall?.callee?.nickname + " (Tôi)"

        directCall?.setLocalVideoView(localView)
        directCall?.setRemoteVideoView(remoteView)

        if (!callManager?.acceptCallSetting!!.camera) {
            localViewContainer?.visibility = View.INVISIBLE
            btnToggleCam!!.setImageResource(R.drawable.ic_cam_ina)
            tvCamStatus!!.text = "Tắt camera"
        }
        if (!callManager?.acceptCallSetting!!.microphone) {
            btnToggleMic!!.setImageResource(R.drawable.ic_mic_ina)
            tvMicStatus!!.text = "Tắt mic"
        }

        btnEndCall!!.setOnClickListener {
            directCall?.end()
        }

        btnOpenChat!!.setOnClickListener {
            chatLoading!!.visibility = View.VISIBLE
            btnOpenChat!!.visibility = View.GONE

            val intent = Intent(this, WebViewCallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
                tvMicStatus!!.text = "Tắt mic"
            } else {
                btnToggleMic!!.setImageResource(R.drawable.ic_mic_atv)
                tvMicStatus!!.text = "Bật mic"
            }
            directCall?.let { it1 -> callManager!!.toggleMic(it1) }
        }

        callManager?.onCallStateChanged = {
            when (it) {
                CallState.CONNECTED -> {
                    Log.d("zzz", "inside callManager.onCallStateChanged")
                    localViewContainer?.visibility = View.VISIBLE
                    remoteView?.visibility = View.VISIBLE
                }
                CallState.ESTABLISHED -> {

                }
                CallState.RECONNECTING -> TODO()
                CallState.RECONNECTED -> TODO()
                CallState.ENDED -> TODO()
            }
        }

//        btnAudioDevices!!.setOnClickListener {
////            audioDialog.show()
//            var device: AudioDevice? = null
//            directCall?.fetchBluetoothDevices()
//            directCall?.availableAudioDevices?.forEach { it ->
//                Log.d("zzz", it.name)
//                if (it == AudioDevice.SPEAKERPHONE) {
//                    device = it
//                }
//            }
//            device?.let { dv ->
//                directCall?.selectAudioDevice(dv) {
//                    Log.d("zzz", "Selected ${dv.name}")
//                }
//            }
//        }
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
            localViewContainer!!.visibility = View.INVISIBLE
            bottomContainer!!.visibility = View.INVISIBLE
            bottomOverlay!!.visibility = View.INVISIBLE
        } else {
            // Restore the full-screen UI.
            localViewContainer!!.visibility = View.VISIBLE
            bottomContainer!!.visibility = View.VISIBLE
            bottomOverlay!!.visibility = View.VISIBLE
        }
    }

    private fun initCallListener() {
        directCall?.setListener(object : DirectCallListener() {
            override fun onConnected(call: DirectCall) {
                if (!call.isRemoteVideoEnabled) {
                    bgAvatar!!.visibility = View.VISIBLE
                }
//                Log.d("zzz1", directCall!!.currentAudioDevice?.name.toString())
//                directCall!!.selectAudioDevice(AudioDevice.EARPIECE) {
//                    Log.d("zzz", directCall!!.currentAudioDevice?.name.toString())
//                }
            }

            override fun onEnded(call: DirectCall) {
                CallService.stopService(this@VideoCallActivity)
                callManager?.callState = "ENDED"
                callManager?.resetCall()

                finish()
            }

            override fun onAudioDeviceChanged(
                call: DirectCall,
                currentAudioDevice: AudioDevice?,
                availableAudioDevices: MutableSet<AudioDevice>
            ) {
                super.onAudioDeviceChanged(call, currentAudioDevice, availableAudioDevices)
            }

            override fun onCustomItemsDeleted(call: DirectCall, deletedKeys: List<String>) {
                super.onCustomItemsDeleted(call, deletedKeys)
            }

            override fun onCustomItemsUpdated(call: DirectCall, updatedKeys: List<String>) {
                super.onCustomItemsUpdated(call, updatedKeys)
            }

            override fun onEstablished(call: DirectCall) {
                super.onEstablished(call)
            }

            @SuppressLint("SetTextI18n")
            override fun onLocalVideoSettingsChanged(call: DirectCall) {
                super.onLocalVideoSettingsChanged(call)
                if (!call.isLocalVideoEnabled) {
                    localViewContainer?.visibility = View.INVISIBLE
                    btnToggleCam!!.setImageResource(R.drawable.ic_cam_ina)
                    tvCamStatus!!.text = "Tắt camera"
                } else {
                    localViewContainer?.visibility = View.VISIBLE
                    btnToggleCam!!.setImageResource(R.drawable.ic_cam_atv)
                    tvCamStatus!!.text = "Bật camera"
                }
            }

            override fun onReconnected(call: DirectCall) {
                super.onReconnected(call)
                callManager!!.finishCurrentActivity()
            }

            override fun onReconnecting(call: DirectCall) {
                super.onReconnecting(call)
                val reconnecting = Intent(applicationContext, IncomingCallActivity::class.java)
                reconnecting.putExtra("isReconnecting", true)
                reconnecting.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(reconnecting)
            }

            override fun onRemoteAudioSettingsChanged(call: DirectCall) {
                super.onRemoteAudioSettingsChanged(call)

            }

            override fun onRemoteRecordingStatusChanged(call: DirectCall) {
                super.onRemoteRecordingStatusChanged(call)
            }

            override fun onRemoteVideoSettingsChanged(call: DirectCall) {
                super.onRemoteVideoSettingsChanged(call)
                if (!call.isRemoteVideoEnabled) {
                    bgAvatar!!.visibility = View.VISIBLE
                } else {
                    bgAvatar!!.visibility = View.INVISIBLE
                }
            }

            override fun onUserHoldStatusChanged(
                call: DirectCall,
                isLocalUser: Boolean,
                isUserOnHold: Boolean
            ) {
                super.onUserHoldStatusChanged(call, isLocalUser, isUserOnHold)
            }
        })
    }

    override fun onBackPressed() {
//        super.onBackPressed()
    }

    private fun formatCallTime(): String {
        val minute = (totalCallTime % 3600) / 60
        val second = (totalCallTime % 60)
        return String.format("%02d:%02d", minute, second)
    }

    private val countdown = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (totalCallTime > 0) {
                totalCallTime -= 1
                tvCallTimeout!!.text = formatCallTime()
            } else {
                mainHandler.removeCallbacks(this)
                directCall!!.end()
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun countdownCallTime() {
        mainHandler.post(countdown)
    }
}