package com.evergreenpro.glasslayers.domain.model

enum class ReportLog(val fileName: String) {
    SYSTEMUI("systemui.log"),
    CRASH("crash.log"),
    FRAMEWORK("xposed.log"),
    APPS("apps.log")
}
