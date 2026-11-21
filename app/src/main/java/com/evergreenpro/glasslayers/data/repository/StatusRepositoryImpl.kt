package com.evergreenpro.glasslayers.data.repository

import com.evergreenpro.glasslayers.data.source.HookReportSource
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.data.source.ReportContract
import com.evergreenpro.glasslayers.data.source.ReportFileSource
import com.evergreenpro.glasslayers.data.source.RootShell
import com.evergreenpro.glasslayers.data.source.XposedServiceSource
import com.evergreenpro.glasslayers.domain.model.FrameworkInfo
import com.evergreenpro.glasslayers.domain.model.HookReport
import com.evergreenpro.glasslayers.domain.model.ReportLog
import com.evergreenpro.glasslayers.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatusRepositoryImpl(
    private val service: XposedServiceSource,
    private val reports: HookReportSource,
    private val files: ReportFileSource
) : StatusRepository {

    override val framework: Flow<FrameworkInfo?> = service.service.map { bound ->
        bound ?: return@map null
        try {
            FrameworkInfo(
                name = bound.frameworkName,
                version = bound.frameworkVersion,
                apiVersion = bound.apiVersion,
                scope = bound.scope
            )
        } catch (_: Throwable) {
            null
        }
    }

    override val hookReport: Flow<HookReport?> = reports.report

    override fun installedAt(): Long = reports.installedAt()

    override fun installedApkPath(): String = reports.installedApkPath()

    override fun requestHookReport() {
        reports.request()
    }

    override suspend fun restartSystemUi(): Boolean {
        service.commit { it.putLong(PrefKeys.RESTART_TOKEN, System.currentTimeMillis()) }
        return RootShell.run("killall " + ReportContract.SYSTEMUI_PACKAGE + " && { killall " + ReportContract.APP_SCOPES.joinToString(" ") + " 2>/dev/null; true; }")
    }

    override suspend fun systemLogs(): Map<ReportLog, String> = files.systemLogs()

    override suspend fun saveReport(target: String, files: Map<String, String>): Boolean = this.files.write(target, files)
}
