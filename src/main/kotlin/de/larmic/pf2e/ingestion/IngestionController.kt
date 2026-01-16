package de.larmic.pf2e.ingestion

import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.job.AsyncJobExecutor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

/**
 * REST controller for triggering vector store ingestion.
 *
 * Provides endpoints to ingest Foundry entries into the vector store
 * for RAG-based similarity search. Operations run asynchronously and
 * return job IDs for status tracking.
 */
@RestController
@RequestMapping("/api/ingestion")
class IngestionController(
    private val ingestionService: IngestionService,
    private val repository: FoundryRawEntryRepository,
    private val jobStore: IngestionJobStore,
    private val asyncJobExecutor: AsyncJobExecutor
) {

    /**
     * Ingest all entries into the vector store.
     *
     * By default, only processes entries that have changed since last ingestion (incremental).
     * Use force=true to perform a full re-index.
     *
     * @param force If true, re-index all entries regardless of vectorization status.
     */
    @PostMapping("/all")
    fun ingestAll(
        @RequestParam(defaultValue = "false") force: Boolean
    ): ResponseEntity<IngestionJob> {
        val job = jobStore.create("ALL")

        asyncJobExecutor.execute {
            try {
                val result = if (force) {
                    ingestionService.ingestAll(job.id)
                } else {
                    ingestionService.ingestPending(job.id)
                }
                jobStore.complete(job.id, result)
            } catch (e: Exception) {
                jobStore.fail(job.id, e.message ?: "Unknown error")
            }
        }

        return ResponseEntity
            .accepted()
            .location(URI.create("/api/ingestion/jobs/${job.id}"))
            .body(job)
    }

    /**
     * Get job status by ID.
     */
    @GetMapping("/jobs/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<IngestionJob> {
        val job = jobStore.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    /**
     * Get all jobs.
     */
    @GetMapping("/jobs")
    fun getAllJobs(): List<IngestionJob> = jobStore.findAll()

    /**
     * Get available foundry types with entry counts.
     */
    @GetMapping("/types")
    fun getAvailableTypes(): ResponseEntity<List<TypeCount>> {
        val types = repository.findAllFoundryTypes()
        val counts = types.map { type ->
            TypeCount(type, repository.countByFoundryType(type))
        }
        return ResponseEntity.ok(counts)
    }

    /**
     * Get ingestion statistics including pending count.
     */
    @GetMapping("/stats")
    fun getStats(): ResponseEntity<IngestionStats> {
        val total = repository.count()
        val pending = repository.countPendingVectorization()
        val vectorized = total - pending

        return ResponseEntity.ok(
            IngestionStats(
                totalEntries = total,
                vectorizedEntries = vectorized,
                pendingEntries = pending
            )
        )
    }
}

data class TypeCount(
    val type: String,
    val count: Long
)

data class IngestionStats(
    val totalEntries: Long,
    val vectorizedEntries: Long,
    val pendingEntries: Long
)
