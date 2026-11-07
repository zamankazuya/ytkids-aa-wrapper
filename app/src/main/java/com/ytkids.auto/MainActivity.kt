package com.ytkids.auto

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private var sessionController: YtKidsSessionController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val controller = YtKidsSessionController(
            uiContext = this,
            isCar = false,
            onExit = {
                sessionController?.destroy()
                finish()
            }
        )
        sessionController = controller
        setContentView(controller.getView())
    }

    override fun onResume() {
        super.onResume()
        sessionController?.syncOnResume()
    }

    override fun onDestroy() {
        sessionController?.destroy()
        sessionController = null
        super.onDestroy()
    }
}
