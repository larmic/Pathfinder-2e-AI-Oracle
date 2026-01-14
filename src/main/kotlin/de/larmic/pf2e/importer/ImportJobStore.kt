package de.larmic.pf2e.importer

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
class ImportJobStore {

    private val jobs = ConcurrentHashMap<UUID, ImportJob>()

    fun create(itemType: String): ImportJob {
        val job = ImportJob(itemType = itemType)
        jobs[job.id] = job
        return job
    }

    fun findById(id: UUID): ImportJob? = jobs[id]

    fun findAll(): List<ImportJob> = jobs.values.toList()

    fun start(id: UUID, totalFiles: Int): ImportJob? {
        return jobs.computeIfPresent(id) { _, job ->
            job.copy(
                status = JobStatus.RUNNING,
                startedAt = Instant.now(),
                progress = job.progress.copy(totalFiles = totalFiles)
            )
        }
    }

    fun updateProgress(id: UUID, processed: Int, skipped: Int): ImportJob? {
        return jobs.computeIfPresent(id) { _, job ->
            job.copy(
                progress = job.progress.copy(
                    processedFiles = processed,
                    skippedFiles = skipped
                )
            )
        }
    }

    fun complete(id: UUID, result: ImportResult): ImportJob? {
        return jobs.computeIfPresent(id) { _, job ->
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
    }

    fun fail(id: UUID, errorMessage: String): ImportJob? {
        return jobs.computeIfPresent(id) { _, job ->
            job.copy(
                status = JobStatus.FAILED,
                completedAt = Instant.now(),
                errorMessage = errorMessage
            )
        }
    }

    fun clear() {
        jobs.clear()
    }
}
