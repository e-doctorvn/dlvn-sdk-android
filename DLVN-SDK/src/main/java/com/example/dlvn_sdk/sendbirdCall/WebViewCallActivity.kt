package com.example.dlvn_sdk.sendbirdCall

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.dlvn_sdk.R
import com.example.dlvn_sdk.store.AppStore
import com.example.dlvn_sdk.webview.SdkWebView
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdVideoView
import com.sendbird.calls.handler.DirectCallListener
import jp.wasabeef.glide.transformations.BlurTransformation

class WebViewCallActivity: AppCompatActivity() {
    private val webView: SdkWebView? = AppStore.webViewInstance
    private var remoteView: SendBirdVideoView? = null
    private var remoteCoverView: ImageView? = null
    private var btnRemoteView: DraggableRelativeLayout? = null
    private val directCall: DirectCall? = CallManager.getInstance()?.directCall

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        if (savedInstanceState == null) {
            webView?.webViewCallActivity = this@WebViewCallActivity

            if (webView != null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container_view, webView)
                    .commit()
            }
        }

        initView()
        initCallListener()
    }

    private fun initView() {
        remoteView = findViewById(R.id.remote_view_wv)
        remoteCoverView = findViewById(R.id.cover_wv)
        btnRemoteView = findViewById(R.id.btn_remote_view_wv)

        btnRemoteView!!.setOnClickListener {
            finish()
        }

        Glide
            .with(this)
            .load(resources.getDrawable(R.drawable.dlvn_city_bg))
            .apply(RequestOptions.bitmapTransform(BlurTransformation(180)))
            .into(remoteCoverView!!)
        remoteCoverView!!.visibility = View.INVISIBLE
    }

    private fun initCallListener() {
        directCall?.setRemoteVideoView(remoteView)
        directCall?.setListener(object : DirectCallListener() {
            override fun onConnected(call: DirectCall) {

            }

            override fun onEnded(call: DirectCall) {
                finish()
                CallManager.getInstance()?.finishCurrentActivity()
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

            override fun onLocalVideoSettingsChanged(call: DirectCall) {
                super.onLocalVideoSettingsChanged(call)

            }

            override fun onReconnected(call: DirectCall) {
                super.onReconnected(call)

            }

            override fun onReconnecting(call: DirectCall) {
                super.onReconnecting(call)

            }

            override fun onRemoteAudioSettingsChanged(call: DirectCall) {
                super.onRemoteAudioSettingsChanged(call)

            }

            override fun onRemoteRecordingStatusChanged(call: DirectCall) {
                super.onRemoteRecordingStatusChanged(call)
            }

            override fun onRemoteVideoSettingsChanged(call: DirectCall) {
                super.onRemoteVideoSettingsChanged(call)
                if (call.isRemoteVideoEnabled) {
                    remoteView!!.visibility = View.VISIBLE
                    remoteCoverView!!.visibility = View.INVISIBLE
                } else {
                    remoteView!!.visibility = View.INVISIBLE
                    remoteCoverView!!.visibility = View.VISIBLE
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
}