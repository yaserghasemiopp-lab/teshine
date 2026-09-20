package com.ashrafi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

private const val SITE_HOST = "teshine.ir"
private const val HOME_URL = "https://teshine.ir"

private val creamColor = Color(0xFFF5E6D3)
private val brownColor = Color(0xFF8D6E63)
private val greenColor = Color(0xFF0EAD69)

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var lastBackPressTime = 0L
    private var isNetworkAvailable = true

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // رنگ نوار بالای گوشی
        window.statusBarColor = android.graphics.Color.rgb(14, 173, 105)

        // آیکون‌ها و ساعت سفید
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = false

        connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        isNetworkAvailable = checkNetwork()

        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                runOnUiThread {
                    isNetworkAvailable = true
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    isNetworkAvailable = checkNetwork()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                runOnUiThread {
                    isNetworkAvailable =
                        networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET
                        )
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                request,
                networkCallback
            )
        } catch (_: Exception) {
        }

        setContent {
            MaterialTheme {
                CompositionRoot()
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    val currentWebView = webView

                    if (currentWebView != null && currentWebView.canGoBack()) {
                        currentWebView.goBack()
                        return
                    }

                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastBackPressTime < 2000) {
                        finish()
                    } else {
                        lastBackPressTime = currentTime
                        Toast.makeText(
                            this@MainActivity,
                            "برای خروج دوباره دکمه بازگشت را بزنید",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun checkNetwork(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities =
                connectivityManager.getNetworkCapabilities(network)

            capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }

        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }

        webView = null

        super.onDestroy()
    }

    @Composable
    private fun CompositionRoot() {

        var showSplash by remember {
            mutableStateOf(true)
        }

        var pageLoaded by remember {
            mutableStateOf(false)
        }

        var offline by remember {
            mutableStateOf(!isNetworkAvailable)
        }

        LaunchedEffect(Unit) {
            delay(900)
            showSplash = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            if (!offline) {

                WebViewContainer(
                    modifier = Modifier.fillMaxSize(),
                    onPageLoaded = {
                        pageLoaded = true
                    },
                    onNetworkError = {
                        offline = true
                    }
                )

                AnimatedVisibility(
                    visible = !pageLoaded || showSplash,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SplashScreen()
                }

                AnimatedVisibility(
                    visible = pageLoaded && !showSplash,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OfflineBanner(
                        visible = !isNetworkAvailable
                    )
                }

            } else {

                OfflineScreen(
                    onRetry = {
                        offline = !checkNetwork()

                        if (!offline) {
                            webView?.reload()
                        }
                    }
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun WebViewContainer(
        modifier: Modifier,
        onPageLoaded: () -> Unit,
        onNetworkError: () -> Unit
    ) {

        AndroidView(
            modifier = modifier,
            factory = { context ->

                WebView(context).apply {

                    webView = this

                    settings.apply {

                        javaScriptEnabled = true

                        domStorageEnabled = true

                        databaseEnabled = true

                        allowContentAccess = true

                        allowFileAccess = true

                        allowFileAccessFromFileURLs = false

                        allowUniversalAccessFromFileURLs = false

                        javaScriptCanOpenWindowsAutomatically = false

                        setSupportMultipleWindows(false)

                        builtInZoomControls = false

                        displayZoomControls = false

                        setSupportZoom(false)

                        useWideViewPort = true

                        loadWithOverviewMode = true

                        mixedContentMode =
                            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        mediaPlaybackRequiresUserGesture = false

                        cacheMode = WebSettings.LOAD_DEFAULT

                        userAgentString =
                            "TeshineApp/1.0 " +
                            WebSettings.getDefaultUserAgent(context)
                    }

                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    setBackgroundColor(
                        android.graphics.Color.WHITE
                    )

                    webViewClient = object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {

                            val url =
                                request?.url?.toString()
                                    ?: return false

                            val host =
                                request.url?.host?.lowercase()
                                    ?: ""

                            return if (
                                host == SITE_HOST ||
                                host == "www.$SITE_HOST"
                            ) {
                                false
                            } else {

                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            request.url
                                        )
                                    )
                                } catch (_: Exception) {
                                }

                                true
                            }
                        }

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            super.onPageStarted(
                                view,
                                url,
                                favicon
                            )
                        }

                        override fun onPageFinished(
                            view: WebView?,
                            url: String?
                        ) {

                            super.onPageFinished(
                                view,
                                url
                            )

                            // غیرفعال کردن انتخاب متن
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        * {
                                            -webkit-user-select: none !important;
                                            user-select: none !important;
                                            -webkit-touch-callout: none !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                })();
                                """.trimIndent(),
                                null
                            )

                            onPageLoaded()

                            prefetchLinks(view)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {

                            super.onReceivedError(
                                view,
                                request,
                                error
                            )

                            if (
                                request?.isForMainFrame == true
                            ) {
                                onNetworkError()
                            }
                        }
                    }

                    loadUrl(HOME_URL)
                }
            },
            update = {
                webView = it
            }
        )
    }

    private fun prefetchLinks(view: WebView?) {

        if (view == null) return

        view.evaluateJavascript(
            """
            (function() {

                var links = Array.from(
                    document.querySelectorAll('a[href]')
                );

                var urls = links
                    .map(function(a) {
                        return a.href;
                    })
                    .filter(function(href) {
                        try {
                            var u = new URL(href);

                            return (
                                u.hostname === '$SITE_HOST' ||
                                u.hostname === 'www.$SITE_HOST'
                            );
                        } catch(e) {
                            return false;
                        }
                    })
                    .slice(0, 6);

                return JSON.stringify(urls);

            })();
            """.trimIndent()
        ) {
            // لینک‌ها برای آماده‌سازی اولیه شناسایی شدند
        }
    }

    @Composable
    private fun SplashScreen() {

        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.logo
                        ),
                        contentDescription = "Teshine",
                        modifier = Modifier.size(150.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "در حال بارگذاری...",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    private fun OfflineBanner(
        visible: Boolean
    ) {

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it }
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it }
            ) + fadeOut()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFFF9800)
                    )
                    .padding(
                        vertical = 8.dp,
                        horizontal = 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "اتصال اینترنت برقرار نیست",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun OfflineScreen(
        onRetry: () -> Unit
    ) {

        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(creamColor)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.logo
                        ),
                        contentDescription = "Teshine",
                        modifier = Modifier.size(130.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Text(
                        text = "📡",
                        fontSize = 48.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "اتصال به اینترنت برقرار نیست",
                        color = brownColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = "لطفاً اتصال اینترنت خود را بررسی کنید و دوباره تلاش کنید.",
                        color = brownColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = greenColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "تلاش مجدد",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
