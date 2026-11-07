package com.ytkids.auto

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var controlBar: LinearLayout
    private val hideHandler = Handler(Looper.getMainLooper())
    private var phoneZoom = 100

    private val hideRunnable = Runnable {
        if (::controlBar.isInitialized) {
            controlBar.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape & fullscreen immersive resolution
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val prefs = getSharedPreferences("ytkids_prefs", Context.MODE_PRIVATE)
        phoneZoom = prefs.getInt("zoom_phone", 100)

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
        ).toInt()

        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
        }

        val btnBack = Button(this).apply {
            text = "< Back"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                if (webView.canGoBack()) webView.goBack()
                resetHideTimer()
            }
        }

        val btnZoomOut = Button(this).apply {
            text = " - "
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                phoneZoom = (phoneZoom - 15).coerceIn(50, 300)
                prefs.edit().putInt("zoom_phone", phoneZoom).apply()
                applyViewportAndZoom()
                resetHideTimer()
            }
        }

        val btnZoomIn = Button(this).apply {
            text = " + "
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                phoneZoom = (phoneZoom + 15).coerceIn(50, 300)
                prefs.edit().putInt("zoom_phone", phoneZoom).apply()
                applyViewportAndZoom()
                resetHideTimer()
            }
        }

        val btnExit = Button(this).apply {
            text = "Exit"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CCFF0000"))
            setOnClickListener {
                finish()
            }
        }

        controlBar.addView(btnBack)
        controlBar.addView(btnZoomOut)
        controlBar.addView(btnZoomIn)
        controlBar.addView(btnExit)

        rootLayout.addView(webView)
        rootLayout.addView(controlBar)
        setContentView(rootLayout)

        setupWebView()
        resetHideTimer()
    }

    private fun applyViewportAndZoom() {
        val js = """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                document.documentElement.style.zoom = '${phoneZoom}%';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun resetHideTimer() {
        controlBar.visibility = View.VISIBLE
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000L)
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
            // Fit to native phone landscape width automatically
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/122.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyViewportAndZoom()
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
