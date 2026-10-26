package com.aslanbek.fitclub

import android.app.Application
import com.aslanbek.fitclub.di.AppContainer

class FitClubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
        container.clock.start()
    }
}
