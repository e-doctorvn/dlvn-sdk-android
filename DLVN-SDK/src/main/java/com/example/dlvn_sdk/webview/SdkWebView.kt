package com.example.dlvn_sdk.webview

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.DlvnSdk
import com.example.dlvn_sdk.R

class SdkWebView: DialogFragment() {
    private lateinit var loading: ConstraintLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var myWebView: WebView
    private lateinit var wvTitle: TextView
    private var buttonBack: Button? = null
    private var buttonNext: Button? = null
    private lateinit var buttonClose: ImageButton
    private lateinit var buttonRefresh: ImageButton
    lateinit var containerErrorNetwork: ConstraintLayout
    lateinit var header: ConstraintLayout
    private var checkTimeoutLoadWebView = false
    var domain = Constants.healthConsultantUrl

    companion object {
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SdkWebView.isVisible = true
        setStyle(STYLE_NO_FRAME, R.style.DialogStyle);
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, null)
    }

    private fun backScreen(): Unit {
        dismiss()
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkConnected(): Boolean {
        val cm =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(
            R.layout.webview,
            container, false
        )
        WebView(requireContext()).clearCache(true)
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        dialog?.window?.statusBarColor = Color.TRANSPARENT
        myWebView = v.findViewById(R.id.webview)
        loadingProgressBar = v.findViewById(R.id.loadingProgressBar)
        loading = v.findViewById(R.id.loading)
        buttonBack = v.findViewById(R.id.buttonBack)
        buttonNext = v.findViewById(R.id.buttonNext)
        header = v.findViewById(R.id.header)
        wvTitle = v.findViewById(R.id.tv_wv_title)
        buttonClose = v.findViewById(R.id.btn_close_wv)
        buttonRefresh = v.findViewById(R.id.btn_refresh)
        containerErrorNetwork = v.findViewById(R.id.containerErrorNetwork)

        myWebView.clearCache(true);
        myWebView.clearFormData();
        myWebView.clearHistory();
        myWebView.clearSslPreferences();
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        buttonBack?.setOnClickListener {
            dismiss()
        }
        buttonNext?.setOnClickListener {
            containerErrorNetwork.visibility = View.GONE
            loading.visibility = View.VISIBLE
            myWebView.reload()
        }
        buttonClose.setOnClickListener {
//            header.visibility = View.GONE
//            myWebView.loadUrl(domain)
            if (myWebView.canGoBack()) {
                myWebView.goBack()
            } else {
                myWebView.removeAllViews();
                myWebView.destroy()
                SdkWebView.isVisible = false
                this.dismiss()
            }
        }
        buttonRefresh.setOnClickListener {
            loading.visibility = View.VISIBLE
            myWebView.reload()
        }

        myWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (errorCode == -2) {
                    requireActivity().runOnUiThread {
                        loading.visibility = View.GONE
                        containerErrorNetwork?.visibility = View.VISIBLE
                    }
                }
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (loading.visibility != View.GONE) {
                    loading.visibility = View.GONE
                }
                checkTimeoutLoadWebView = true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Thread {
                    try {
                        Thread.sleep(30000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    if (!checkTimeoutLoadWebView) {
                        if (isVisible) {
                            requireActivity().runOnUiThread {
                                loading.visibility = View.GONE
                                containerErrorNetwork.visibility = View.VISIBLE
                            }
                        }
                    }
                }.start()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString()
                return if (url == null || url.startsWith("http://") || url.startsWith("https://")) false else try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
                    view!!.context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    Log.d(DlvnSdk.LOG_TAG, "shouldOverrideUrlLoading Exception:$e")
                    true
                }
            }
        }

        val webSettings: WebSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.setSupportMultipleWindows(true)
        webSettings.setGeolocationEnabled(true)
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.databaseEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.setGeolocationEnabled(true)
        webSettings.loadWithOverviewMode = true
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        cookieManager.setAcceptThirdPartyCookies(myWebView, true)

        myWebView.loadUrl(
            domain,
            mapOf(
                "Content-Type" to "application/json",
            )
        )
        if (!isNetworkConnected()) {
            containerErrorNetwork.visibility = View.VISIBLE
        }
        return v
    }

    override fun onDestroy() {
        myWebView.removeAllViews();
        myWebView.destroy()
        SdkWebView.isVisible = false
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                if (myWebView.url == domain) {
                    dismiss()
                } else if (myWebView.canGoBack()) {
                    myWebView.goBack()
                } else {
                    dismiss()
                }
            }
        }
    }
}