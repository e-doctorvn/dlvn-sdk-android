package com.edoctor.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.DimensionUtils
import com.edoctor.dlvn_sdk.model.Dimension
import com.edoctor.dlvn_sdk.store.AppStore
import com.edoctor.dlvn_sdk.webview.SdkWebView
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdVideoView
import jp.wasabeef.glide.transformations.BlurTransformation

class WebViewCallActivity: AppCompatActivity() {
    private val webView: SdkWebView = SdkWebView(AppStore.sdkInstance!!)
    private var remoteView: SendBirdVideoView? = null
    private var remoteCoverView: ImageView? = null
    private var btnRemoteView: DraggableRelativeLayout? = null
    private var btnEndCall: ImageButton? = null
    private var btnToggleCam: ImageButton? = null
    private var btnToggleMic: ImageButton? = null

    private var callManager: CallManager? = CallManager.getInstance()
    private val directCall: DirectCall? = callManager?.directCall

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView.domain = getChatRoomUrl()
        if (savedInstanceState == null) {
            webView.hideLoading = true
            webView.webViewCallActivity = this@WebViewCallActivity

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_view, webView)
                .commit()
        }

        initView()
        initCallEventListener()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initView() {
        remoteView = findViewById(R.id.remote_view_wv)
        remoteCoverView = findViewById(R.id.cover_wv)
        btnRemoteView = findViewById(R.id.btn_remote_view_wv)

        btnEndCall = findViewById(R.id.btn_end_call_pip)
        btnToggleMic = findViewById(R.id.btn_toggle_mic_pip)
        btnToggleCam = findViewById(R.id.btn_toggle_cam_pip)
        btnEndCall!!.setOnClickListener {
            callManager?.endEclinicCall()
            directCall?.end()
        }
        btnToggleCam!!.setOnClickListener {
            callManager?.toggleCam(directCall!!)
        }
        btnToggleMic!!.setOnClickListener {
            if (directCall!!.isLocalAudioEnabled) {
                btnToggleMic!!.setImageResource(R.drawable.ic_mic_ina)
            } else {
                btnToggleMic!!.setImageResource(R.drawable.ic_mic_atv)
            }
            callManager?.toggleMic(directCall)
        }

        btnRemoteView!!.setOnClickListener {
            finish()
        }

        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(remoteCoverView!!)

        if (directCall!!.isRemoteVideoEnabled) {
            remoteCoverView!!.visibility = View.INVISIBLE
        } else {
            remoteCoverView!!.visibility = View.VISIBLE
        }
        if (directCall.isLocalAudioEnabled) {
            btnToggleMic!!.setImageResource(R.drawable.ic_mic_atv)
        } else {
            btnToggleMic!!.setImageResource(R.drawable.ic_mic_ina)
        }

        val screenSize: Dimension = DimensionUtils.getScreenSize(this)

        val layout = btnRemoteView!!.layoutParams
        layout.width = (screenSize.width * 0.27f).toInt()
        layout.height = ((screenSize.width * 0.27f) * 1.5f).toInt()
    }

    private fun initCallEventListener() {
        directCall?.setRemoteVideoView(remoteView)
        callManager?.onCallStateChanged = {
            when (it) {
                Constants.CallState.ESTABLISHED -> {}
                Constants.CallState.CONNECTED -> {}
                Constants.CallState.RECONNECTING -> {}
                Constants.CallState.RECONNECTED -> {}
                Constants.CallState.ENDED -> finish()
            }
        }

        callManager?.onCallActionChanged = {
            when (it) {
                Constants.CallAction.REMOTE_VIDEO -> {
                    if (directCall?.isRemoteVideoEnabled!!) {
                        remoteView!!.visibility = View.VISIBLE
                        remoteCoverView!!.visibility = View.INVISIBLE
                    } else {
                        remoteView!!.visibility = View.INVISIBLE
                        remoteCoverView!!.visibility = View.VISIBLE
                    }
                }
                else -> {
                    if (!directCall?.isLocalVideoEnabled!!) {
                        btnToggleCam!!.setImageResource(R.drawable.ic_cam_ina)
                    } else {
                        btnToggleCam!!.setImageResource(R.drawable.ic_cam_atv)
                    }
                }
            }
        }

        callManager?.closeWebViewActivity = {
            finish()
        }
    }

    private fun getChatRoomUrl(): String {
        return if (EdoctorDlvnSdk.environment == Constants.Env.SANDBOX) {
            Constants.healthConsultantUrlDev
        } else {
            Constants.healthConsultantUrlProd
        } + "/phong-tu-van?channel=${callManager?.appointmentDetail?.channelUrl}"
    }
}