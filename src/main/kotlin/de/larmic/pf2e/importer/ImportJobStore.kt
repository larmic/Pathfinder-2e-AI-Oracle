package de.larmic.pf2e.importer

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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
