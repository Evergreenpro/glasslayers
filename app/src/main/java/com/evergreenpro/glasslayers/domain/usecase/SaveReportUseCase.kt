package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.StatusRepository

class SaveReportUseCase(private val repository: StatusRepository) {
    suspend operator fun invoke(target: String, report: String): Boolean {
        val files = linkedMapOf(REPORT_FILE to report)
        repository.systemLogs().forEach { (kind, text) -> files[kind.fileName] = text }
        return repository.saveReport(target, files)
    }

    private companion object {
        const val REPORT_FILE = "report.txt"
    }
}
