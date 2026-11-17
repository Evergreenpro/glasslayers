package com.drdisagree.glasslayer.data.source

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import com.drdisagree.glasslayer.data.mapper.SyncedPrefKeys

class PrefsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != PrefsContract.METHOD_READ) return null
        enforceCaller()
        val context = context ?: return null
        val stored = context.getSharedPreferences(PrefKeys.GROUP, Context.MODE_PRIVATE).all
        val keys = if (arg == null) SyncedPrefKeys.all else setOf(arg).filter { it in SyncedPrefKeys.all }
        val values = Bundle()
        val removed = ArrayList<String>()
        keys.forEach { key -> if (!values.putValue(key, stored[key])) removed += key }
        return Bundle().apply {
            putBundle(PrefsContract.EXTRA_VALUES, values)
            putStringArrayList(PrefsContract.EXTRA_REMOVED, removed)
        }
    }

    private fun enforceCaller() {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) return
        val packages = callingPackage?.let { listOf(it) }
            ?: context?.packageManager?.getPackagesForUid(uid)?.toList().orEmpty()
        if (packages.none { it in PrefsContract.SCOPE }) throw SecurityException("Caller $uid is not in the module scope")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

private fun Bundle.putValue(key: String, value: Any?): Boolean {
    when (value) {
        is Boolean -> putBoolean(key, value)
        is Float -> putFloat(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is String -> putString(key, value)
        else -> return false
    }
    return true
}
