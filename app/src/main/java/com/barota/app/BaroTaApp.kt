package com.barota.app

import android.app.Application
import com.barota.app.di.AppContainer

class BaroTaApp : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
