package com.drdisagree.glasslayer

import android.app.Application
import com.drdisagree.glasslayer.data.source.PrefsChangeNotifier
import com.drdisagree.glasslayer.data.source.XposedServiceSource
import com.drdisagree.glasslayer.di.AppContainer

class GlassLayerApp : Application() {

    lateinit var container: AppContainer
        private set

    private lateinit var prefsNotifier: PrefsChangeNotifier

    override fun onCreate() {
        super.onCreate()
        XposedServiceSource.init()
        container = AppContainer(this)
        prefsNotifier = PrefsChangeNotifier(this).also { it.start() }
    }
}
