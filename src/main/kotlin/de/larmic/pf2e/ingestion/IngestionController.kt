package de.larmic.pf2e.ingestion

import de.larmic.pf2e.domain.FoundryRawEntryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val jobStore: IngestionJobStore
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Ingest all entries of a specific type.
     *
     * @param incremental If true, only process entries that have changed since last ingestion.
     *
     * Example: POST /api/ingestion/type/spell
     * Example: POST /api/ingestion/type/spell?incremental=true
     */
    @PostMapping("/type/{foundryType}")
    fun ingestByType(
        @PathVariable foundryType: String,
        @RequestParam(defaultValue = "false") incremental: Boolean
    ): ResponseEntity<IngestionJob> {
        val job = jobStore.create(foundryType.uppercase())

        scope.launch {
            try {
                val result = if (incremental) {
                    ingestionService.ingestPendingByType(foundryType, job.id)
                } else {
                    ingestionService.ingestByType(foundryType, job.id)
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
     * Ingest all entries (full re-index).
     * Warning: This may take 15-30 minutes with Ollama embeddings.
     *
     * @param incremental If true, only process entries that have changed since last ingestion.
     *
     * Example: POST /api/ingestion/all
     * Example: POST /api/ingestion/all?incremental=true
     */
    @PostMapping("/all")
    fun ingestAll(
        @RequestParam(defaultValue = "false") incremental: Boolean
    ): ResponseEntity<IngestionJob> {
        val job = jobStore.create("ALL")

        scope.launch {
            try {
                val result = if (incremental) {
                    ingestionService.ingestPending(job.id)
                } else {
                    ingestionService.ingestAll(job.id)
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
     *
     * Example: GET /api/ingestion/jobs/{id}
     */
    @GetMapping("/jobs/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<IngestionJob> {
        val job = jobStore.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    /**
     * Get all jobs.
     *
     * Example: GET /api/ingestion/jobs
     */
    @GetMapping("/jobs")
    fun getAllJobs(): List<IngestionJob> = jobStore.findAll()

    /**
     * Get available foundry types for ingestion.
     *
     * Example: GET /api/ingestion/types
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
     *
     * Example: GET /api/ingestion/stats
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
