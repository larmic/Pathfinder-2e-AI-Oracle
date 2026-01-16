package de.larmic.pf2e.ingestion

import com.fasterxml.uuid.Generators
import de.larmic.pf2e.job.Job
import de.larmic.pf2e.job.JobStatus
import java.time.Instant
import java.util.*

data class IngestionJob(
    override val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    val foundryType: String,
    override val status: JobStatus = JobStatus.PENDING,
    override val createdAt: Instant = Instant.now(),
    override val startedAt: Instant? = null,
    override val completedAt: Instant? = null,
    val progress: IngestionProgress = IngestionProgress(),
    val result: IngestionResult? = null,
    override val errorMessage: String? = null
) : Job

data class IngestionProgress(
    val totalEntries: Int = 0,
    val processedEntries: Int = 0,
    val errors: Int = 0
) {
    val percentComplete: Int
        get() = if (totalEntries > 0) (processedEntries * 100) / totalEntries else 0
}
