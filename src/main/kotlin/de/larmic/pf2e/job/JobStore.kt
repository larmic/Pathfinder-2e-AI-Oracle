package de.larmic.pf2e.job

import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Base interface for jobs that can be tracked by a JobStore.
 */
interface Job {
    val id: UUID
    val status: JobStatus
    val createdAt: Instant
    val startedAt: Instant?
    val completedAt: Instant?
    val errorMessage: String?
}

/**
 * Abstract base class for in-memory job stores.
 *
 * Provides common functionality for storing and retrieving jobs.
 * Subclasses implement type-specific operations.
 *
 * IMPORTANT: This is an ephemeral store - all job data is lost on application restart.
 */
abstract class JobStore<T : Job> {

    protected val jobs = ConcurrentHashMap<UUID, T>()

    fun findById(id: UUID): T? = jobs[id]

    fun findAll(): List<T> = jobs.values.toList()

    fun clear() {
        jobs.clear()
    }

    protected fun store(job: T): T {
        jobs[job.id] = job
        return job
    }

    protected fun update(id: UUID, updateFn: (T) -> T): T? {
        return jobs.computeIfPresent(id) { _, job -> updateFn(job) }
    }
}
