package com.drdisagree.glasslayer.data.source

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.drdisagree.glasslayer.data.mapper.SyncedPrefKeys

class PrefsChangeNotifier(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when {
            key == null -> notify(PrefsContract.CHANGED_URI)
            key in SyncedPrefKeys.all -> notify(PrefsContract.changedUri(key))
        }
    }

    fun start() {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun notify(uri: Uri) {
        try {
            appContext.contentResolver.notifyChange(uri, null)
        } catch (_: Throwable) {
        }
    }
}
