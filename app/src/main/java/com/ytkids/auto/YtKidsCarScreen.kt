package com.ytkids.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class YtKidsCarScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("YouTube Kids Web Wrapper Active.\nTap below to open or control playback from your phone display.")
            .setTitle("YT Kids")
            .addAction(
                Action.Builder()
                    .setTitle("Open on Phone / Mirror")
                    .setOnClickListener {
                        // Brings phone app to foreground to interact with WebView controls
                        carContext.startCarApp(
                            carContext.packageManager.getLaunchIntentForPackage(carContext.packageName)!!
                        )
                    }
                    .build()
            )
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
