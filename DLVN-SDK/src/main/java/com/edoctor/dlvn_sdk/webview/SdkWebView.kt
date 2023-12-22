package com.edoctor.dlvn_sdk.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.os.Handler
import android.os.Message
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.PermissionManager
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date

open class SdkWebView(sdk: EdoctorDlvnSdk): DialogFragment() {
    private lateinit var loading: ConstraintLayout
    private lateinit var loadingProgressBar: ProgressBar
    lateinit var myWebView: WebView
    private var buttonBack: Button? = null
    private var buttonNext: Button? = null
    lateinit var containerErrorNetwork: ConstraintLayout

    private var sdkInstance: EdoctorDlvnSdk
    private var checkTimeoutLoadWebView = false
    var domain = Constants.healthConsultantUrlDev
    var defaultDomain = Constants.healthConsultantUrlDev
    private val FCR = 99
    private var mCM: String? = null
    var hideLoading: Boolean = false
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    init {
        sdkInstance = sdk
    }
    companion object {
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Companion.isVisible = true
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dialog?.window?.decorView?.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
        setStyle(STYLE_NO_FRAME, R.style.EDRDialogStyle)
    }

    @SuppressLint("ServiceCast")
    private fun isNetworkConnected(): Boolean {
        val cm =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    fun reload() {
        hideLoading = true
        myWebView.loadUrl(domain, mapOf("Content-Type" to "application/json"))
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
        try {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            myWebView = v.findViewById(R.id.webview)
            loadingProgressBar = v.findViewById(R.id.loadingProgressBar)
            loading = v.findViewById(R.id.loading)
            buttonBack = v.findViewById(R.id.buttonBack)
            buttonNext = v.findViewById(R.id.buttonNext)
            containerErrorNetwork = v.findViewById(R.id.containerErrorNetwork)

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

            myWebView.webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val result = view!!.hitTestResult
                    val data = result.extra
                    val context = view.context

                    if (data.toString().contains(Constants.dlvnDomain)) {
                        view.loadUrl(data.toString() + "?from=eDoctor&screen=eDoctorHome")
                        return true
                    } else {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data))
                        context.startActivity(browserIntent)
                    }
                    return false
                }
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
                    mCM = photoFile.toUri().toString()


                    val captureImgUri =
                        FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().applicationContext.packageName + ".com.edoctor.application.provider",
                            photoFile
                        )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, captureImgUri)
                    takePictureIntent.putExtra("return-data", false)
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
                @SuppressLint("WebViewClientOnReceivedSslError")
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    var dialog: AlertDialog? = null
                    val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    builder.setMessage(getString(R.string.ssl_error_confirm_msg))
                    builder.setPositiveButton("Tiếp tục") { _, _ ->
                        handler!!.proceed()
                        dialog?.dismiss()
                    }
                    builder.setNegativeButton("Đóng") { _, _ ->
                        handler!!.cancel()
                        dialog?.dismiss()
                    }
                    dialog = builder.create()
                    dialog.show()
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
                    Handler().postDelayed({
                        if (loading.visibility != View.GONE && hideLoading) {
                            hideLoading = false
                            loading.visibility = View.GONE
                            checkTimeoutLoadWebView = true
                        }
                        super.onPageFinished(view, url)
                    },2000)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (EdoctorDlvnSdk.edrAccessToken != null && EdoctorDlvnSdk.dlvnAccessToken != null) {
                        view?.evaluateJavascript("sessionStorage.setItem(\"accessTokenEdr\", \"${EdoctorDlvnSdk.edrAccessToken}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"upload_token\", \"${EdoctorDlvnSdk.edrAccessToken}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"accessTokenDlvn\", \"${EdoctorDlvnSdk.dlvnAccessToken}\");") {}
                    }
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
                    try {
                        if (url.toString().contains(Constants.dlvnDomain)) {
                            view?.loadUrl(url.toString() + "?from=eDoctor&screen=eDoctorHome")
                            return false
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            view!!.context.startActivity(intent)
                        }
                        return true
                    } catch (e: Exception) {
                        Log.d(EdoctorDlvnSdk.LOG_TAG, "shouldOverrideUrlLoading Exception:$e")
                    }
                    return true
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
                mapOf("Content-Type" to "application/json")
            )
            if (!isNetworkConnected()) {
                containerErrorNetwork.visibility = View.VISIBLE
            }
        } catch (e: Error) {

        }
        return v
    }

    override fun onDestroy() {
        myWebView.removeAllViews();
        myWebView.destroy()
        Companion.isVisible = false
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            override fun onBackPressed() {
                if (myWebView.url == domain) {
                    selfClose()
                } else if (myWebView.canGoBack()) {
                    myWebView.goBack()
                } else {
                    dismiss()
                }
            }
        }
    }

    fun selfClose() {
        this.dismiss()
        hideLoading = false
        myWebView.removeAllViews()
        myWebView.destroy()
        Companion.isVisible = false
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
        requestPermissionLauncher?.let {
            PermissionManager.handleRequestPermission(
                requireActivity(),
                Manifest.permission.CAMERA,
                it
            )
        }
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
                    } else { // Take photo
                        mCM?.let {
                            results = arrayOf(Uri.parse(it))
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
