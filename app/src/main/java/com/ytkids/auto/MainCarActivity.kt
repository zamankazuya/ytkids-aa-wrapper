package com.ytkids.auto

import android.os.Bundle
import com.google.android.apps.auto.sdk.CarActivity

class MainCarActivity : CarActivity() {

    private var sessionController: YtKidsSessionController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
        super.onCreate(savedInstanceState)

        try {
            setIgnoreConfigChanges(0xFFFF)
            carUiController?.statusBarController?.hideTitle()
        } catch (_: Throwable) {
        }

        val controller = YtKidsSessionController(
            uiContext = this,
            isCar = true,
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
