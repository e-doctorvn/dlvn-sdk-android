package com.example.dlvn_sdk.sendbirdCall

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.R
import com.example.dlvn_sdk.store.AppStore
import com.example.dlvn_sdk.webview.SdkWebView
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdVideoView
import com.sendbird.calls.handler.DirectCallListener
import jp.wasabeef.glide.transformations.BlurTransformation

class WebViewCallActivity: AppCompatActivity() {
    private val webView: SdkWebView = SdkWebView(AppStore.sdkInstance!!)
    private var remoteView: SendBirdVideoView? = null
    private var remoteCoverView: ImageView? = null
    private var btnRemoteView: DraggableRelativeLayout? = null
    private var callManager: CallManager? = CallManager.getInstance()
    private val directCall: DirectCall? = callManager?.directCall

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        if (savedInstanceState == null) {
            webView.webViewCallActivity = this@WebViewCallActivity

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container_view, webView)
                .commit()
        }

        initView()
        initCallEventListener()
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
        if (directCall!!.isRemoteVideoEnabled) {
            remoteCoverView!!.visibility = View.INVISIBLE
        } else {
            remoteCoverView!!.visibility = View.VISIBLE
        }
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
                else -> {}
            }
        }

        callManager?.closeWebViewActivity = {
            finish()
        }
    }
}