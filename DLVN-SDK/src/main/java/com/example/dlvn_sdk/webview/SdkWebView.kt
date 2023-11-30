package com.example.dlvn_sdk.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.R
import com.example.dlvn_sdk.helper.PermissionManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

open class SdkWebView(sdk: EdoctorDlvnSdk): DialogFragment() {
    private lateinit var loading: ConstraintLayout
    private lateinit var loadingProgressBar: ProgressBar
    lateinit var myWebView: WebView
    private lateinit var wvTitle: TextView
    private var buttonBack: Button? = null
    private var buttonNext: Button? = null
    private lateinit var buttonClose: ImageButton
    private lateinit var buttonRefresh: ImageButton
    lateinit var containerErrorNetwork: ConstraintLayout
    lateinit var header: ConstraintLayout

    private var sdkInstance: EdoctorDlvnSdk
    private var checkTimeoutLoadWebView = false
    var domain = Constants.healthConsultantUrlDev
    private var mCM: String? = null
    private var mUM: ValueCallback<Uri>? = null
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private val FCR = 99
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

    init {
        sdkInstance = sdk
    }
    companion object {
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SdkWebView.isVisible = true
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dialog?.window?.decorView?.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        setStyle(STYLE_NO_FRAME, R.style.EDRDialogStyle)
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkConnected(): Boolean {
        val cm =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val fragment = manager.findFragmentByTag(tag)
        if (fragment != null && fragment.isAdded) {
            manager.beginTransaction().remove(fragment).commit()
        }
        super.show(manager, tag)
    }

    @SuppressLint("SetJavaScriptEnabled", "InternalInsetResource")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(
            R.layout.webview,
            container, false
        )
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

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
        buttonClose.setColorFilter(Color.argb(255, 255, 255, 255))
        buttonRefresh.setColorFilter(Color.argb(255, 255, 255, 255))

        header.visibility = View.GONE

        if (EdoctorDlvnSdk.needClearCache) {
            clearCacheAndCookies(requireContext())
            EdoctorDlvnSdk.needClearCache = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(myWebView, true)

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
                selfClose()
            }
        }
        buttonRefresh.setOnClickListener {
            loading.visibility = View.VISIBLE
            myWebView.reload()
        }

        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                activity!!.runOnUiThread {
                    request.grant(request.resources)
                }
            }

            @RequiresApi(Build.VERSION_CODES.M)
            @SuppressLint("QueryPermissionsNeeded")
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (mUMA != null) {
                    mUMA!!.onReceiveValue(null)
                }
                mUMA = filePathCallback

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile: File = createImageFile()
                mCM = Uri.fromFile(photoFile).toString()

                val captureImgUri =
                    FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().applicationContext.packageName + ".com.example.application.provider",
                        photoFile
                    )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, captureImgUri)
                takePictureIntent.putExtra("return-data", true)
                takePictureIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                val intentArray: Array<Intent> = arrayOf(takePictureIntent)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
                startActivityForResult(chooserIntent, FCR)
                requireActivity().runOnUiThread { requestCameraPermission() }
                return true
            }
        }


        myWebView.webViewClient = object : WebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                if (view?.title?.contains("https") == false) {
                    wvTitle.text = formatWebTitle(view.title!!)
                }
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
//                super.onReceivedSslError(view, handler, error)
                handler?.proceed()
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (errorCode == -2) {
                    requireActivity().runOnUiThread {
                        loading.visibility = View.GONE
                        containerErrorNetwork.visibility = View.VISIBLE
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
                if (EdoctorDlvnSdk.edrAccessToken != null && EdoctorDlvnSdk.dlvnAccessToken != null) {
                    view?.evaluateJavascript("document.cookie=\"accessToken=${EdoctorDlvnSdk.edrAccessToken}; path=/\"") {}
                    view?.evaluateJavascript("document.cookie=\"upload_token=${EdoctorDlvnSdk.edrAccessToken}; path=/\"") {}
                    view?.evaluateJavascript("document.cookie=\"accessTokenDlvn=${EdoctorDlvnSdk.dlvnAccessToken}; path=/\"") {}
                }
                Thread {
                    try {
                        Thread.sleep(40000)
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
                    Log.d(EdoctorDlvnSdk.LOG_TAG, "shouldOverrideUrlLoading Exception:$e")
                    true
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }

        val jsInterface = JsInterface(this, sdkInstance)
        val webSettings: WebSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.blockNetworkLoads = false
        webSettings.blockNetworkImage = false
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.setSupportMultipleWindows(true)
        webSettings.setGeolocationEnabled(true)
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.databaseEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.allowFileAccess = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cookieManager.setAcceptThirdPartyCookies(myWebView, true)
        myWebView.setLayerType(View.LAYER_TYPE_NONE, null)
        myWebView.addJavascriptInterface(jsInterface, "Android")
        myWebView.addJavascriptInterface(jsInterface, "AndroidEdoctorCallback")

        myWebView.loadUrl(
            domain,
            mapOf(
                "Content-Type" to "application/json",
//                "Authorization" to EdoctorDlvnSdk.edrAccessToken
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

    private fun formatWebTitle(title: String): String {
        if (title.contains(" -")) {
            return title.substring(0, title.indexOf(" -"))
        }
        return  title
    }

    fun selfClose() {
        myWebView.removeAllViews();
        myWebView.destroy()
        SdkWebView.isVisible = false
        this.dismiss()
        super.onDestroy()
        this.onDestroy()
    }

    fun clearCacheAndCookies(context: Context) {
        WebView(context).clearCache(true)
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        if (::myWebView.isInitialized) {
            myWebView.clearCache(true)
            myWebView.clearFormData()
            myWebView.clearHistory()
            myWebView.clearSslPreferences()
        }
    }

    private fun requestCameraPermission() {
        PermissionManager.handleRequestPermission(
            requireActivity(),
            Manifest.permission.CAMERA,
            requestPermissionLauncher
        )
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        @SuppressLint("SimpleDateFormat") val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
        this.openFileChooser(uploadMsg, "*/*")
    }

    private fun openFileChooser(
        uploadMsg: ValueCallback<Uri?>?,
        acceptType: String?
    ) {
        this.openFileChooser(uploadMsg, acceptType, null)
    }

    private fun openFileChooser(
        uploadMsg: ValueCallback<Uri?>?,
        acceptType: String?,
        capture: String?
    ) {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        requireActivity().startActivityForResult(
            Intent.createChooser(i, "File Browser"),
            99
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)

        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FCR) {
                if (intent?.dataString == null) { // Multi select || Take photo
                    if (intent != null) {
                        if (intent.clipData != null) { // Multi select
                            results = Array(intent.clipData!!.itemCount) {
                                intent.clipData!!.getItemAt(it).uri
                            }
                        } else if (mCM != null) { // Take photo
                            results = arrayOf(Uri.parse(mCM))
                        }
                    }
                } else { // Single select
                    val dataString = intent.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
            }
        }

        mUMA!!.onReceiveValue(results)
        mUMA = null
    }
}
