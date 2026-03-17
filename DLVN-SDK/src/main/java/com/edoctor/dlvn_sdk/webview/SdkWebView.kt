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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlin.math.max

class SdkWebView(sdk: EdoctorDlvnSdk): DialogFragment() {
    private lateinit var loading: ConstraintLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var webViewContainer: View
    private lateinit var statusBarScrim: View
    lateinit var myWebView: WebView
    private var buttonBack: Button? = null
    private var buttonNext: Button? = null
    lateinit var containerErrorNetwork: ConstraintLayout

    private var sdkInstance: EdoctorDlvnSdk
    var webViewCallActivity: Context? = null
    private var checkTimeoutLoadWebView = false
    var domain = Constants.healthConsultantUrlDev
    var defaultDomain = Constants.healthConsultantUrlDev
    private var capturedImagePath: String? = null
    var hideLoading: Boolean = false
    private var mUMA: ValueCallback<Array<Uri>>? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestMultipleCallPermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var requestMultiplePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var originalActivityStatusBarColor: Int? = null
    private var originalActivityLightStatusBar: Boolean? = null
    private var originalActivityHadTranslucentStatus: Boolean? = null
    private var originalActivityHadDrawsSystemBarBackgrounds: Boolean? = null

    init {
        sdkInstance = sdk
        AppStore.webViewInstance = this
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 99
        var isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Companion.isVisible = true
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
            R.layout.dlvn_sdk_webview,
            container, false
        )
        try {
            dialog?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.blue_primary)
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
            }
            applyActivityStatusBarStyle()

            myWebView = requireNotNull(v.findViewById<WebView?>(R.id.webview)) {
                missingRequiredViewMessage("webview")
            }
            loadingProgressBar = requireNotNull(v.findViewById<ProgressBar?>(R.id.loadingProgressBar)) {
                missingRequiredViewMessage("loadingProgressBar")
            }
            loading = requireNotNull(v.findViewById<ConstraintLayout?>(R.id.loading)) {
                missingRequiredViewMessage("loading")
            }
            buttonBack = v.findViewById(R.id.buttonBack)
            buttonNext = v.findViewById(R.id.buttonNext)
            containerErrorNetwork = requireNotNull(v.findViewById<ConstraintLayout?>(R.id.containerErrorNetwork)) {
                missingRequiredViewMessage("containerErrorNetwork")
            }
            webViewContainer = requireNotNull(v.findViewById<View?>(R.id.webview_container)) {
                missingRequiredViewMessage("webview_container")
            }
            statusBarScrim = requireNotNull(v.findViewById<View?>(R.id.status_bar_scrim)) {
                missingRequiredViewMessage("status_bar_scrim")
            }
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

            ViewCompat.setOnApplyWindowInsetsListener(v) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                val left = max(systemBars.left, cutout.left)
                val top = max(systemBars.top, cutout.top)
                val right = max(systemBars.right, cutout.right)
                val bottom = max(systemBars.bottom, ime.bottom)
                applyInsets(webViewContainer, left, top, right, bottom)
                applyInsets(loading, left, top, right, bottom)
                applyInsets(containerErrorNetwork, left, top, right, bottom)
                applyStatusBarScrim(top)
                insets
            }
            ViewCompat.requestApplyInsets(v)

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
                        if (loading.visibility != View.GONE) {
                            hideLoading = false
                            loading.visibility = View.GONE
                            EdoctorDlvnSdk.debounceWVShortLink = false
                        }
                            super.onPageFinished(view, url)
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
        } catch (_: Error) { }
        return v
    }

    private fun applyInsets(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        view.setPadding(left, top, right, bottom)
    }

    private fun applyStatusBarScrim(height: Int) {
        val layoutParams = statusBarScrim.layoutParams
        if (layoutParams.height != height) {
            layoutParams.height = height
            statusBarScrim.layoutParams = layoutParams
        }
        statusBarScrim.visibility = if (height > 0) View.VISIBLE else View.GONE
    }

    private fun applyActivityStatusBarStyle() {
        activity?.window?.let { window ->
            if (originalActivityStatusBarColor == null) {
                originalActivityStatusBarColor = window.statusBarColor
            }
            if (originalActivityHadTranslucentStatus == null) {
                originalActivityHadTranslucentStatus =
                    (window.attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) != 0
            }
            if (originalActivityHadDrawsSystemBarBackgrounds == null) {
                originalActivityHadDrawsSystemBarBackgrounds =
                    (window.attributes.flags and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) != 0
            }
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (originalActivityLightStatusBar == null) {
                originalActivityLightStatusBar = controller.isAppearanceLightStatusBars
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.blue_primary)
            controller.isAppearanceLightStatusBars = false
        }
    }

    private fun restoreActivityStatusBarStyle() {
        activity?.window?.let { window ->
            originalActivityHadTranslucentStatus?.let { hadFlag ->
                if (hadFlag) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }
            originalActivityHadDrawsSystemBarBackgrounds?.let { hadFlag ->
                if (hadFlag) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                }
            }
            originalActivityStatusBarColor?.let { color ->
                window.statusBarColor = color
            }
            originalActivityLightStatusBar?.let { isLightStatusBar ->
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLightStatusBar
            }
        }
        originalActivityStatusBarColor = null
        originalActivityLightStatusBar = null
        originalActivityHadTranslucentStatus = null
        originalActivityHadDrawsSystemBarBackgrounds = null
    }

    private fun missingRequiredViewMessage(viewId: String): String {
        return "Missing required view '$viewId' in SDK layout. Check for resource name collisions in host app."
    }

    override fun onDestroy() {
        restoreActivityStatusBarStyle()
        if (::myWebView.isInitialized) {
            myWebView.removeAllViews()
            myWebView.destroy()
        }
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
        hideLoading = false
        dismissAllowingStateLoss()
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

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)

        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK && requestCode == FILE_CHOOSER_REQUEST) {
            results = when {
                intent?.clipData != null -> Array(intent.clipData!!.itemCount) {
                    intent.clipData!!.getItemAt(it).uri
                }
                intent?.dataString != null -> arrayOf(Uri.parse(intent.dataString))
                capturedImagePath != null -> arrayOf(Uri.parse(capturedImagePath))
                else -> null
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
                    capturedImagePath = photoFile.toUri().toString()

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
                startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST)
            }
        } catch (_: Error) { }
    }

    private fun onRequestCallPermissionsResult(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        val results = permissions.values.toList()
        val hasNotificationPermission = results.size > 2 && results[2]
        if (hasNotificationPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationHelper.initialize(EdoctorDlvnSdk.context)
        }
    }
}
