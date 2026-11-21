package com.evergreenpro.glasslayers.domain.repository

import com.evergreenpro.glasslayers.domain.model.FrameworkInfo
import com.evergreenpro.glasslayers.domain.model.HookReport
import com.evergreenpro.glasslayers.domain.model.ReportLog
import kotlinx.coroutines.flow.Flow

interface StatusRepository {
    val framework: Flow<FrameworkInfo?>
    val hookReport: Flow<HookReport?>
    fun installedAt(): Long
    fun installedApkPath(): String
    fun requestHookReport()
    suspend fun restartSystemUi(): Boolean
    suspend fun systemLogs(): Map<ReportLog, String>
    suspend fun saveReport(target: String, files: Map<String, String>): Boolean
}
