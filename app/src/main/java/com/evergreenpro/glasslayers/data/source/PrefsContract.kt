package com.evergreenpro.glasslayers.data.source

import android.net.Uri

object PrefsContract {
    const val AUTHORITY = "com.evergreenpro.glasslayers.prefs"
    const val METHOD_READ = "read"
    const val EXTRA_VALUES = "values"
    const val EXTRA_REMOVED = "removed"
    const val ACTION_CHANGED = "com.evergreenpro.glasslayers.action.PREFS_CHANGED"
    const val PERMISSION = "com.evergreenpro.glasslayers.permission.PREFS"
    private const val PATH_CHANGED = "changed"

    val BASE_URI: Uri = Uri.Builder().scheme("content").authority(AUTHORITY).build()
    val CHANGED_URI: Uri = BASE_URI.buildUpon().appendPath(PATH_CHANGED).build()
    val SCOPE: Set<String> = setOf(ReportContract.SYSTEMUI_PACKAGE) + ReportContract.APP_SCOPES

    fun changedUri(key: String): Uri = CHANGED_URI.buildUpon().appendPath(key).build()

    fun keyOf(uri: Uri?): String? = uri?.pathSegments?.getOrNull(1)
}
