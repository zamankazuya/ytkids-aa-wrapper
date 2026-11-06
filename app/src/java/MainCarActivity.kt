package com.ytkids.auto

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import com.google.android.apps.auto.sdk.CarActivity

class MainCarActivity : CarActivity() {

    private lateinit var webView: WebView
    private lateinit var btnExit: Button
    private val hideHandler = Handler(Looper.getMainLooper())

    private val hideRunnable = Runnable {
        btnExit.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_main)

        // Request full screen / hide Android Auto status bar if supported
        carUiController.statusBarController.hideAppHeader()

        webView = findViewById(R.id.webview) as WebView
        btnExit = findViewById(R.id.btn_exit) as Button

        setupExitButton()
        setupWebView()
    }

    private fun setupExitButton() {
        btnExit.setOnClickListener {
            hideHandler.removeCallbacks(hideRunnable)
            webView.destroy()
            finish()
        }
        // Start initial 5-second countdown
        resetHideTimer()
    }

    private fun resetHideTimer() {
        btnExit.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true

            // Force Desktop Chrome User-Agent so YouTube Kids loads the web player
            // instead of redirecting to the mobile app promo page
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/122.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }
        }
        webView.webChromeClient = WebChromeClient()

        // Show the Exit button whenever the screen is touched, then hide after 5s
        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetHideTimer()
            }
            false // Return false so WebView still processes clicks and scrolling
        }

        webView.loadUrl("https://www.youtubekids.com")
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        CookieManager.getInstance().flush()
        super.onDestroy()
    }
}
