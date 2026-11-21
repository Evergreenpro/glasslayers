package com.evergreenpro.glasslayers.domain.model

data class HookReport(
    val timestamp: Long,
    val framework: String,
    val systemUiVersion: String,
    val buildId: String,
    val moduleUpdatedAt: Long,
    val moduleLoadedAt: Long,
    val moduleApkPath: String,
    val uptimeMs: Long,
    val captureAvailable: Boolean,
    val mods: List<ModReport>,
    val broken: List<String>,
    val errors: List<String>,
    val logs: List<String>,
    val safeMode: Boolean = false
) {
    /**
     * Whether SystemUI is still running an older build than the one installed.
     *
     * The install time is the reliable signal: the path SystemUI reports for the module can differ from the
     * app's own path on some frameworks even when the freshest code is loaded, which left the restart banner
     * stuck forever. The path is only used when either time is unknown.
     */
    fun isStale(installedApkPath: String, installedAt: Long = 0L): Boolean = when {
        moduleUpdatedAt > 0L && installedAt > 0L -> moduleUpdatedAt < installedAt
        else -> moduleApkPath.isEmpty() || moduleApkPath != installedApkPath
    }
}
