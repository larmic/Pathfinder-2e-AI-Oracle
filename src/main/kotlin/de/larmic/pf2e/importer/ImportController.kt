package de.larmic.pf2e.importer

import de.larmic.pf2e.cleanup.CleanupResult
import de.larmic.pf2e.cleanup.OrphanCleanupService
import de.larmic.pf2e.cleanup.OrphanInfo
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.job.AsyncJobExecutor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * REST controller for importing Foundry VTT PF2e data from GitHub.
 *
 * Provides endpoints to trigger imports and track job status.
 * Operations run asynchronously and return job IDs for status tracking.
 */
@RestController
@RequestMapping("/api/import")
class ImportController(
    private val importService: FoundryImportService,
    private val repository: FoundryRawEntryRepository,
    private val jobStore: ImportJobStore,
    private val asyncJobExecutor: AsyncJobExecutor,
    private val orphanCleanupService: OrphanCleanupService
) {

    /**
     * Import all categories from GitHub.
     *
     * Only downloads files that are new or have changed (SHA comparison).
     */
    @PostMapping("/all")
    fun importAll(): ResponseEntity<ImportJob> {
        val job = jobStore.create("ALL")

        asyncJobExecutor.execute {
            try {
                val result = importService.importAll(job.id)
                jobStore.complete(job.id, result)
            } catch (e: Exception) {
                jobStore.fail(job.id, e.message ?: "Unknown error")
            }
        }

        return ResponseEntity
            .accepted()
            .location(URI.create("/api/import/jobs/${job.id}"))
            .body(job)
    }

    /**
     * Get available categories for import.
     */
    @GetMapping("/categories")
    fun getCategories(): List<String> = importService.getAvailableCategories()

    /**
     * Get job status by ID.
     */
    @GetMapping("/jobs/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<ImportJob> {
        val job = jobStore.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    /**
     * Get all jobs.
     */
    @GetMapping("/jobs")
    fun getAllJobs(): List<ImportJob> = jobStore.findAll()

    /**
     * Get import statistics.
     */
    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        val types = repository.findAllFoundryTypes()
        return mapOf(
            "total" to repository.count(),
            "byType" to types.associateWith { repository.countByFoundryType(it) }
        )
    }

    /**
     * Preview orphaned entries without deleting them.
     *
     * Orphans are entries in the database whose source files no longer exist in GitHub.
     */
    @GetMapping("/cleanup/preview")
    fun previewCleanup(): List<OrphanInfo> = orphanCleanupService.detectOrphans()

    /**
     * Clean up orphaned entries from database and vector store.
     *
     * Deletes entries that no longer exist in the GitHub repository.
     */
    @PostMapping("/cleanup")
    fun cleanupOrphans(): ResponseEntity<CleanupResult> {
        val result = orphanCleanupService.cleanupOrphans()
        return ResponseEntity.ok(result)
    }
}
