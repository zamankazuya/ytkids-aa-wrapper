package com.ytkids.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class YtKidsCarService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_VALIDATORS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return YtKidsCarSession()
    }
}

class YtKidsCarSession : Session() {
    override fun onCreateScreen(intent: Intent): androidx.car.app.Screen {
        return YtKidsCarScreen(carContext)
    }
}
