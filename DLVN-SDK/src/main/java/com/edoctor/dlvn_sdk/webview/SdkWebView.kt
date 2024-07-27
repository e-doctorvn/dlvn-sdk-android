package com.edoctor.dlvn_sdk.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.ProgressBar
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
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.helper.PermissionManager
import com.edoctor.dlvn_sdk.store.AppStore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class SdkWebView(sdk: EdoctorDlvnSdk): DialogFragment() {
    private lateinit var loading: ConstraintLayout
    private lateinit var loadingProgressBar: ProgressBar
    lateinit var myWebView: WebView
    private var buttonBack: Button? = null
    private var buttonNext: Button? = null
    lateinit var containerErrorNetwork: ConstraintLayout

    private var sdkInstance: EdoctorDlvnSdk
    var webViewCallActivity: Context? = null
    private var checkTimeoutLoadWebView = false
    var domain = Constants.healthConsultantUrlDev
    var defaultDomain = Constants.healthConsultantUrlDev
    private val FCR = 99
    private var mCM: String? = null
    var hideLoading: Boolean = false
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestMultipleCallPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var requestMultiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        sdkInstance = sdk
        AppStore.webViewInstance = this
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
        requestMultiplePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions -> onRequestPermissionsResult(permissions)}
        requestMultipleCallPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions -> onRequestCallPermissionsResult(permissions)}
        setStyle(STYLE_NO_FRAME, R.style.EDRDialogStyle)
    }

    override fun onResume() {
        super.onResume()
        if (hideLoading && webViewCallActivity != null) {
            loading.visibility = View.GONE
        }
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
            myWebView.overScrollMode = View.OVER_SCROLL_NEVER

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
                    view?.evaluateJavascript("sessionStorage.setItem(\"sdkSupportConsultant\", \"${true}\");") {}
                    view?.evaluateJavascript("sessionStorage.setItem(\"sdkSupportVideoCall\", \"${true}\");") {}
                    try {
                        checkTimeoutLoadWebView = true
                        Handler().postDelayed({
                            if (loading.visibility != View.GONE && hideLoading) {
                                hideLoading = false
                                loading.visibility = View.GONE
                                EdoctorDlvnSdk.debounceWVShortLink = false
                            }
                            super.onPageFinished(view, url)
                        }, 2250)
                        requireActivity().runOnUiThread {
                            if (!EdoctorDlvnSdk.dlvnAccessToken.isNullOrEmpty()) {
                                requestCameraAndMicrophonePermissionForVideoCall()
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("zzz", e.toString())
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (EdoctorDlvnSdk.edrAccessToken != null && EdoctorDlvnSdk.dlvnAccessToken != null) {
                        view?.evaluateJavascript("sessionStorage.setItem(\"accessTokenEdr\", \"${EdoctorDlvnSdk.edrAccessToken}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"upload_token\", \"${EdoctorDlvnSdk.edrAccessToken}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"accessTokenDlvn\", \"${EdoctorDlvnSdk.dlvnAccessToken}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"sdkSupportConsultant\", \"${true}\");") {}
                        view?.evaluateJavascript("sessionStorage.setItem(\"sdkSupportVideoCall\", \"${true}\");") {}
                    }
                    if (EdoctorDlvnSdk.accountExist == false) {
                        view?.evaluateJavascript("sessionStorage.setItem(\"consent\", ${true});") {}
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
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                requireActivity().runOnUiThread {
                    if (myWebView.url == domain) {
                        selfClose()
                    }
                    if (!myWebView.canGoBack()) {
                        selfClose()
                    }
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

    fun openAppInStore() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.dConnectStoreUrl)))
    }

    private fun requestCameraPermission() {
        requestMultiplePermissionLauncher?.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    private fun requestPostNotificationPermission() {
        requestPermissionLauncher?.let {
            PermissionManager.handleRequestPermission(
                requireActivity(),
                Manifest.permission.POST_NOTIFICATIONS,
                it
            )
            NotificationHelper.initialize(requireContext())
        }
    }

    fun requestCameraAndMicrophonePermissionForVideoCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestMultipleCallPermissionLauncher?.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            requestMultipleCallPermissionLauncher?.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                )
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
        hideLoading = true
    }

    private fun onRequestPermissionsResult(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        try {
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)

            val results = permissions.entries.map { it.value }
            if (results.isNotEmpty()) {
                val availableBelowAndroid11 = Build.VERSION.SDK_INT < 30 && results[0] && results[1]
                val availableAboveAndroid11 = Build.VERSION.SDK_INT >= 30 && results[0]

                if (availableAboveAndroid11 || availableBelowAndroid11) {
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

                    val intentArray: Array<Intent> = arrayOf(takePictureIntent)

                    if (webViewCallActivity == null) {
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    }
                }
                // SELECT IMAGES
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Chọn ảnh từ")
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                startActivityForResult(chooserIntent, FCR)
            }
            return
        } catch (_: Error) {

        }
    }

    private fun onRequestCallPermissionsResult(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        try {
            // 0 - Cam, 1 - Mic, 2 - Notification
            val results = permissions.entries.map { it.value }

            if (results.isNotEmpty() && results.size > 2 && results[2]) {
                NotificationHelper.initialize(EdoctorDlvnSdk.context)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationHelper.initialize(EdoctorDlvnSdk.context)
            }
//            if (Build.MANUFACTURER.equals("Google", true)) {
//                var dialog: AlertDialog? = null
//                val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//                builder.setTitle(getString(R.string.need_show_on_lockscreen_permission_label))
//                builder.setMessage(getString(R.string.request_show_on_lockscreen_permission_msg))
//                builder.setPositiveButton(getString(R.string.end_time_ok_label)) { _, _ ->
//                    dialog?.dismiss()
//                    PermissionManager.checkAndRequestShowOnLockScreenXiaomiDevice(requireActivity())
//                }
//                builder.setNegativeButton("Đóng") { _, _ ->
//                    dialog?.dismiss()
//                }
//                dialog = builder.create()
//                dialog.show()
//                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.gray_primary))
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.dlvn_primary))
//            }

            return
        } catch (_: Error) {

        }
    }
}
