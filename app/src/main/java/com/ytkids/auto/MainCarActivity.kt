package com.ytkids.auto

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import com.google.android.apps.auto.sdk.CarActivity

class MainCarActivity : CarActivity() {

    private lateinit var webView: WebView
    private lateinit var btnExit: Button
    private val hideHandler = Handler(Looper.getMainLooper())

    private val hideRunnable = Runnable {
        if (::btnExit.isInitialized) {
            btnExit.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safely hide status bar / app header if supported by the SDK
        try {
            carUiController?.statusBarController?.hideTitle()
        } catch (_: Throwable) {
        }

        val ctx = baseContext

        // 1. Create fullscreen FrameLayout container programmatically
        val rootLayout = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // 2. Create fullscreen WebView
        webView = WebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 3. Create floating Exit button in top-right corner
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, ctx.resources.displayMetrics
        ).toInt()

        btnExit = Button(ctx).apply {
            text = "Exit"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CCFF0000"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
        }

        rootLayout.addView(webView)
        rootLayout.addView(btnExit)

        setContentView(rootLayout)

        setupExitButton()
        setupWebView()
    }

    private fun setupExitButton() {
        btnExit.setOnClickListener {
            hideHandler.removeCallbacks(hideRunnable)
            try {
                webView.loadUrl("about:blank")
                webView.destroy()
            } catch (_: Throwable) {
            }
            System.exit(0)
        }
        resetHideTimer()
    }

    private fun resetHideTimer() {
        btnExit.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000L)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Force Desktop Chrome User-Agent so YouTube Kids loads the web player
        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/122.0.0.0 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }
        }
        webView.webChromeClient = WebChromeClient()

        webView.setOnTouchListener { _, event ->
            if (event != null && event.action == MotionEvent.ACTION_DOWN) {
                resetHideTimer()
            }
            false
        }

        webView.loadUrl("https://www.youtubekids.com")
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        CookieManager.getInstance().flush()
        super.onDestroy()
    }
}
