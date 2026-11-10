package com.drdisagree.glasslayer.data.source

object ReportContract {
    const val APP_PACKAGE = "com.drdisagree.glasslayer"
    const val SYSTEMUI_PACKAGE = "com.android.systemui"
    val APP_SCOPES = listOf("com.android.settings", "com.google.android.packageinstaller", "com.android.packageinstaller")
    const val ACTION_REPORT = "com.drdisagree.glasslayer.action.HOOK_REPORT"
    const val ACTION_REQUEST = "com.drdisagree.glasslayer.action.REQUEST_HOOK_REPORT"
    const val EXTRA_REPORT = "report"
}
