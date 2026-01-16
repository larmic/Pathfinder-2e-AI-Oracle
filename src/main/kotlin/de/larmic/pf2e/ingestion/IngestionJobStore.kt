package de.larmic.pf2e.ingestion

import de.larmic.pf2e.job.JobStatus
import de.larmic.pf2e.job.JobStore
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * In-memory store for ingestion jobs.
 *
 * IMPORTANT: This is an ephemeral store - all job data is lost on application restart.
 * This is intentional for the current use case where:
 * - Jobs are short-lived (ingestion runs take minutes to hours)
 * - Historical job data is not critical
 * - Simplicity is preferred over persistence
 */
@Component
class IngestionJobStore : JobStore<IngestionJob>() {

    fun create(foundryType: String): IngestionJob =
        store(IngestionJob(foundryType = foundryType))

    fun start(id: UUID, totalEntries: Int): IngestionJob? = update(id) { job ->
        job.copy(
            status = JobStatus.RUNNING,
            startedAt = Instant.now(),
            progress = job.progress.copy(totalEntries = totalEntries)
        )
    }

    fun updateProgress(id: UUID, processed: Int, errors: Int): IngestionJob? = update(id) { job ->
        job.copy(
            progress = job.progress.copy(
                processedEntries = processed,
                errors = errors
            )
        )
    }

    fun complete(id: UUID, result: IngestionResult): IngestionJob? = update(id) { job ->
        job.copy(
            status = JobStatus.COMPLETED,
            completedAt = Instant.now(),
            result = result,
            progress = job.progress.copy(
                processedEntries = result.processed,
                errors = result.errors
            )
        )
    }

    fun fail(id: UUID, errorMessage: String): IngestionJob? = update(id) { job ->
        job.copy(
            status = JobStatus.FAILED,
            completedAt = Instant.now(),
            errorMessage = errorMessage
        )
    }
}
