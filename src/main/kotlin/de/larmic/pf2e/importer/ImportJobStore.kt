package de.larmic.pf2e.importer

import de.larmic.pf2e.job.JobStatus
import de.larmic.pf2e.job.JobStore
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * In-memory store for import jobs.
 *
 * IMPORTANT: This is an ephemeral store - all job data is lost on application restart.
 * This is intentional for the current use case where:
 * - Jobs are short-lived (import runs take minutes, not days)
 * - Historical job data is not critical
 * - Simplicity is preferred over persistence
 *
 * For production use cases requiring job history persistence, consider:
 * - Creating an ImportJobEntity JPA entity
 * - Adding a Flyway migration for the jobs table
 * - Replacing ConcurrentHashMap with JPA repository
 */
@Component
class ImportJobStore : JobStore<ImportJob>() {

    fun create(itemType: String): ImportJob =
        store(ImportJob(itemType = itemType))

    fun start(id: UUID, totalFiles: Int): ImportJob? = update(id) { job ->
        job.copy(
            status = JobStatus.RUNNING,
            startedAt = Instant.now(),
            progress = job.progress.copy(totalFiles = totalFiles)
        )
    }

    fun updateProgress(id: UUID, processed: Int, skipped: Int): ImportJob? = update(id) { job ->
        job.copy(
            progress = job.progress.copy(
                processedFiles = processed,
                skippedFiles = skipped
            )
        )
    }

    fun complete(id: UUID, result: ImportResult): ImportJob? = update(id) { job ->
        job.copy(
            status = JobStatus.COMPLETED,
            completedAt = Instant.now(),
            result = result,
            progress = job.progress.copy(
                processedFiles = result.totalFiles,
                skippedFiles = result.skipped
            )
        )
    }

    fun fail(id: UUID, errorMessage: String): ImportJob? = update(id) { job ->
        job.copy(
            status = JobStatus.FAILED,
            completedAt = Instant.now(),
            errorMessage = errorMessage
        )
    }
}
