package com.evergreenpro.glasslayers.data.source

object ReportContract {
    const val APP_PACKAGE = "com.evergreenpro.glasslayers"
    const val SYSTEMUI_PACKAGE = "com.android.systemui"
    val APP_SCOPES = listOf("com.android.settings", "com.google.android.packageinstaller", "com.android.packageinstaller", "com.google.android.inputmethod.latin")
    const val ACTION_REPORT = "com.evergreenpro.glasslayers.action.HOOK_REPORT"
    const val ACTION_REQUEST = "com.evergreenpro.glasslayers.action.REQUEST_HOOK_REPORT"
    const val EXTRA_REPORT = "report"
}
