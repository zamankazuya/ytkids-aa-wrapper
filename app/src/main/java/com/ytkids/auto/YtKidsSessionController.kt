package com.ytkids.auto

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class YtKidsSessionController(
    private val uiContext: Context,
    private val isCar: Boolean,
    private val onExit: () -> Unit
) {
    private val appContext: Context = uiContext.applicationContext
    private val prefs = appContext.getSharedPreferences("ytkids_sync_store", Context.MODE_PRIVATE)
    private val rootLayout: FrameLayout = FrameLayout(uiContext)
    private lateinit var controlBar: LinearLayout
    private var webView: WebView? = null

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val hideHandler = Handler(Looper.getMainLooper())
    private val zoomKey = if (isCar) "zoom_car" else "zoom_phone"
    private var currentZoom = prefs.getInt(zoomKey, if (isCar) 125 else 100)

    private val hideRunnable = Runnable {
        if (::controlBar.isInitialized) {
            controlBar.visibility = View.GONE
        }
    }

    init {
        requestAudioFocus()
        buildUi()
    }

    fun getView(): View = rootLayout

    private fun requestAudioFocus() {
        try {
            if (audioFocusRequest == null) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            // Pause video temporarily during phone calls or voice prompts
                            webView?.evaluateJavascript(
                                "document.querySelectorAll('video').forEach(v => v.pause());",
                                null
                            )
                        }
                    }
                    .build()
            }
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } catch (_: Throwable) {
        }
    }

    private fun abandonAudioFocus() {
        try {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } catch (_: Throwable) {
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun buildUi() {
        rootLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootLayout.setBackgroundColor(Color.BLACK)

        val createdWebView = try {
            WebView(uiContext)
        } catch (_: Throwable) {
            try {
                WebView(appContext)
            } catch (e: Throwable) {
                showErrorOnScreen("WebView init error: ${e.message}")
                return
            }
        }
        webView = createdWebView

        createdWebView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        createdWebView.setBackgroundColor(Color.BLACK)

        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 12f, uiContext.resources.displayMetrics
        ).toInt()

        controlBar = LinearLayout(uiContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
        }

        val btnBack = createOverlayButton("Back", "#AA333333") {
            if (createdWebView.canGoBack()) {
                createdWebView.goBack()
            }
            resetHideTimer()
        }

        val btnZoomOut = createOverlayButton(" - ", "#AA333333") {
            changeZoom(-15)
            resetHideTimer()
        }

        val btnZoomIn = createOverlayButton(" + ", "#AA333333") {
            changeZoom(15)
            resetHideTimer()
        }

        val btnExit = createOverlayButton("Exit", "#CCFF0000") {
            hideHandler.removeCallbacks(hideRunnable)
            onExit()
        }

        controlBar.addView(btnBack)
        controlBar.addView(btnZoomOut)
        controlBar.addView(btnZoomIn)
        controlBar.addView(btnExit)

        rootLayout.addView(createdWebView)
        rootLayout.addView(controlBar)

        configureWebView(createdWebView)
        resetHideTimer()
    }

    private fun createOverlayButton(label: String, bgHex: String, onClick: () -> Unit): Button {
        return Button(uiContext).apply {
            text = label
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(bgHex))
            setOnClickListener { onClick() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configureWebView(wv: WebView) {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(wv, true)
        } catch (_: Throwable) {
        }

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/122.0.0.0 Safari/537.36"
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: android.webkit.JsResult?
            ): Boolean {
                result?.confirm()
                return true
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!url.isNullOrEmpty() && url.startsWith("http")) {
                    prefs.edit().putString("last_url", url).apply()
                }
                applyViewportAndZoom()
                requestAudioFocus()
                try {
                    CookieManager.getInstance().flush()
                } catch (_: Throwable) {
                }
            }
        }

        wv.setOnTouchListener { _, event ->
            if (event != null && event.action == MotionEvent.ACTION_DOWN) {
                requestAudioFocus()
                resetHideTimer()
            }
            false
        }

        val startUrl = prefs.getString("last_url", "https://www.youtubekids.com")
            ?: "https://www.youtubekids.com"
        wv.loadUrl(startUrl)
    }

    private fun changeZoom(delta: Int) {
        currentZoom = (currentZoom + delta).coerceIn(50, 300)
        prefs.edit().putInt(zoomKey, currentZoom).apply()
        applyViewportAndZoom()
    }

    private fun applyViewportAndZoom() {
        val js = """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    if (document.head) document.head.appendChild(meta);
                }
                if (meta) {
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                }
                if (document.documentElement) {
                    document.documentElement.style.zoom = '${currentZoom}%';
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    fun syncOnResume() {
        requestAudioFocus()
        currentZoom = prefs.getInt(zoomKey, if (isCar) 125 else 100)
        val syncedUrl = prefs.getString("last_url", null)
        val currentUrl = webView?.url
        if (!syncedUrl.isNullOrEmpty() && syncedUrl != currentUrl) {
            webView?.loadUrl(syncedUrl)
        } else {
            applyViewportAndZoom()
        }
        resetHideTimer()
    }

    private fun resetHideTimer() {
        if (::controlBar.isInitialized) {
            controlBar.visibility = View.VISIBLE
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 5000L)
        }
    }

    private fun showErrorOnScreen(msg: String) {
        val tv = TextView(uiContext).apply {
            text = msg
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }
        rootLayout.addView(tv)
    }

    fun destroy() {
        hideHandler.removeCallbacks(hideRunnable)
        abandonAudioFocus()
        try {
            CookieManager.getInstance().flush()
            webView?.loadUrl("about:blank")
            webView?.destroy()
        } catch (_: Throwable) {
        }
        webView = null
    }
}
