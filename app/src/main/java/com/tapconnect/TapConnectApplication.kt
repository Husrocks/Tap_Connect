package com.tapconnect

import android.app.Application
import android.content.Context

class TapConnectApplication : Application() {
    companion object {
        private var instance: TapConnectApplication? = null

        fun getContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize things like DI, Bluetooth managers, etc.
    }
}
