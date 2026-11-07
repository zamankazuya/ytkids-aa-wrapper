package com.ytkids.auto

import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.LinearLayout
import com.google.android.apps.auto.sdk.CarActivity

class MainCarActivity : CarActivity() {

    private lateinit var webView: WebView
    private lateinit var controlBar: LinearLayout
    private val hideHandler = Handler(Looper.getMainLooper())
    private var currentZoom = 125 // Default 125% zoom

    private val hideRunnable = Runnable {
        if (::controlBar.isInitialized) {
            controlBar.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            carUiController?.statusBarController?.hideTitle()
        } catch (_: Throwable) {
        }

        val ctx = baseContext
        val prefs = ctx.getSharedPreferences("ytkids_prefs", Context.MODE_PRIVATE)
        currentZoom = prefs.getInt("zoom_percent", 125)

        val rootLayout = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 12f, ctx.resources.displayMetrics
        ).toInt()

        // Top-right container holding [< Back], [-], [+], and [Exit]
        controlBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
        }

        val btnBack = Button(ctx).apply {
            text = "< Back"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                if (webView.canGoBack()) {
                    webView.goBack()
                }
                resetHideTimer()
            }
        }

        val btnZoomOut = Button(ctx).apply {
            text = " - "
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                changeZoom(-15, prefs)
                resetHideTimer()
            }
        }

        val btnZoomIn = Button(ctx).apply {
            text = " + "
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA333333"))
            setOnClickListener {
                changeZoom(15, prefs)
                resetHideTimer()
            }
        }

        val btnExit = Button(ctx).apply {
            text = "Exit"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CCFF0000"))
            setOnClickListener {
                hideHandler.removeCallbacks(hideRunnable)
                try {
                    webView.loadUrl("about:blank")
                    webView.destroy()
                } catch (_: Throwable) {
                }
                System.exit(0)
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

    private fun changeZoom(delta: Int, prefs: android.content.SharedPreferences) {
        currentZoom = (currentZoom + delta).coerceIn(50, 300)
        prefs.edit().putInt("zoom_percent", currentZoom).apply()
        applyCssZoom()
    }

    private fun applyCssZoom() {
        webView.evaluateJavascript(
            "document.body.style.zoom = '${currentZoom}%';",
            null
        )
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

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false

        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/122.0.0.0 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyCssZoom()
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
