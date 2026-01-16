package de.larmic.pf2e.importer

import com.fasterxml.uuid.Generators
import de.larmic.pf2e.job.Job
import de.larmic.pf2e.job.JobStatus
import java.time.Instant
import java.util.*

data class ImportJob(
    override val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    val itemType: String,
    override val status: JobStatus = JobStatus.PENDING,
    override val createdAt: Instant = Instant.now(),
    override val startedAt: Instant? = null,
    override val completedAt: Instant? = null,
    val progress: ImportProgress = ImportProgress(),
    val result: ImportResult? = null,
    override val errorMessage: String? = null
) : Job

data class ImportProgress(
    val totalFiles: Int = 0,
    val processedFiles: Int = 0,
    val skippedFiles: Int = 0
) {
    val percentComplete: Int
        get() = if (totalFiles > 0) (processedFiles * 100) / totalFiles else 0
}
