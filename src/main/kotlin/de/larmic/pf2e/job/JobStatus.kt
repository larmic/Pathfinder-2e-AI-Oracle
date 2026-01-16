package de.larmic.pf2e.job

/**
 * Status of an async job.
 */
enum class JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
