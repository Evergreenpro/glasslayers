package com.drdisagree.glasslayer

import android.app.Application
import com.drdisagree.glasslayer.data.source.PrefsChangeNotifier
import com.drdisagree.glasslayer.data.source.PrefsStorage
import com.drdisagree.glasslayer.data.source.XposedServiceSource
import com.drdisagree.glasslayer.di.AppContainer

class GlassLayerApp : Application() {

    @Volatile
    private var current: AppContainer? = null
    private var prefsNotifier: PrefsChangeNotifier? = null

    val container: AppContainer
        get() = current ?: prepare()

    override fun onCreate() {
        super.onCreate()
        XposedServiceSource.init()
        if (PrefsStorage.isUnlocked(this)) prepare()
    }

    @Synchronized
    private fun prepare(): AppContainer = current ?: AppContainer(this).also { created ->
        prefsNotifier = PrefsChangeNotifier(this).also { it.start() }
        current = created
    }
}
