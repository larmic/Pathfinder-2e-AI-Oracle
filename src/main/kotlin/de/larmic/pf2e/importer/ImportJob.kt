package de.larmic.pf2e.importer

import com.fasterxml.uuid.Generators
import java.time.Instant
import java.util.*

data class ImportJob(
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    val itemType: String,
    val status: JobStatus = JobStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val progress: ImportProgress = ImportProgress(),
    val result: ImportResult? = null,
    val errorMessage: String? = null
)

data class ImportProgress(
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val skippedFiles: Int = 0
) {
    val percentComplete: Int
        get() = if (totalFiles > 0) (processedFiles * 100) / totalFiles else 0
}

enum class JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
