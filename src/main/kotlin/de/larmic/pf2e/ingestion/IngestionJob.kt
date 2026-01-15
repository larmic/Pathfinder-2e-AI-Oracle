package de.larmic.pf2e.ingestion

import com.fasterxml.uuid.Generators
import de.larmic.pf2e.importer.JobStatus
import java.time.Instant
import java.util.*

data class IngestionJob(
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    val foundryType: String,
    val status: JobStatus = JobStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val progress: IngestionProgress = IngestionProgress(),
    val result: IngestionResult? = null,
    val errorMessage: String? = null
)

data class IngestionProgress(
    val totalEntries: Int = 0,
    val processedEntries: Int = 0,
    val errors: Int = 0
) {
    val percentComplete: Int
        get() = if (totalEntries > 0) (processedEntries * 100) / totalEntries else 0
}
