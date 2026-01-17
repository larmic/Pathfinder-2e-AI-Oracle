package de.larmic.pf2e.ingestion

import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service for ingesting Foundry raw entries into the vector store.
 *
 * Converts raw JSON content to embeddings with metadata for RAG-based
 * similarity search. Uses parallel batch processing with virtual threads
 * for efficient handling of large datasets (~28,000 entries).
 */
@Service
class IngestionService(
    private val repository: FoundryRawEntryRepository,
    private val vectorStore: VectorStore,
    private val metadataExtractor: MetadataExtractor,
    private val documentBuilder: DocumentBuilder,
    private val jobStore: IngestionJobStore,
    private val ingestionProperties: IngestionProperties
) {

    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    companion object {
        private const val LOG_INTERVAL = 500
    }

    /**
     * Ingest all entries of a specific type (e.g., "spell", "feat").
     */
    fun ingestByType(foundryType: String, jobId: UUID? = null): IngestionResult {
        log.info("Starting ingestion for type: {}", foundryType)
        val entries = repository.findAllByFoundryType(foundryType)
        return ingestEntries(entries, "type=$foundryType", jobId, markVectorized = true)
    }

    /**
     * Ingest all entries (full re-index).
     * Warning: This may take 15-30 minutes with Ollama embeddings.
     */
    fun ingestAll(jobId: UUID? = null): IngestionResult {
        log.info("Starting full ingestion of all entries")
        val entries = repository.findAll()
        return ingestEntries(entries, "all", jobId, markVectorized = true)
    }

    /**
     * Ingest only entries that have not been vectorized yet or have changed.
     * Uses vectorizedSha to track which entries need processing.
     */
    fun ingestPending(jobId: UUID? = null): IngestionResult {
        log.info("Starting incremental ingestion of pending entries")
        val entries = repository.findAllPendingVectorization()
        return ingestEntries(entries, "pending", jobId, markVectorized = true)
    }

    /**
     * Ingest only pending entries of a specific type.
     */
    fun ingestPendingByType(foundryType: String, jobId: UUID? = null): IngestionResult {
        log.info("Starting incremental ingestion for type: {}", foundryType)
        val entries = repository.findPendingVectorizationByType(foundryType)
        return ingestEntries(entries, "pending type=$foundryType", jobId, markVectorized = true)
    }

    /**
     * Ingest a single entry by its database entity.
     */
    fun ingestEntry(entry: FoundryRawEntry) {
        // Check if it's a journal entry
        if (documentBuilder.isJournal(entry)) {
            val documents = documentBuilder.buildJournalDocuments(entry)
            if (documents.isNotEmpty()) {
                vectorStore.add(documents)
                repository.markAsVectorized(entry.id)
                log.debug("Ingested journal '{}' with {} pages", entry.name, documents.size)
            }
            return
        }

        // Normal entry processing
        val metadata = metadataExtractor.extractMetadata(entry.rawJsonContent, entry.foundryType)
        val document = documentBuilder.buildDocument(entry, metadata)
        vectorStore.add(listOf(document))
        repository.markAsVectorized(entry.id)
        log.debug("Ingested single entry: {} ({})", entry.name, entry.foundryType)
    }

    private fun ingestEntries(
        entries: List<FoundryRawEntry>,
        context: String,
        jobId: UUID?,
        markVectorized: Boolean
    ): IngestionResult {
        val startTime = Instant.now()
        val processed = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val total = entries.size

        log.info("Found {} entries to ingest ({}) with parallelism={}, batchSize={}",
            total, context, ingestionProperties.maxParallelEmbeddings, ingestionProperties.batchSize)

        jobId?.let { jobStore.start(it, total) }

        val semaphore = Semaphore(ingestionProperties.maxParallelEmbeddings)
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        entries.chunked(ingestionProperties.batchSize).forEach { batch ->
            try {
                // Parallel document building with semaphore throttling
                val futures = batch.map { entry ->
                    executor.submit<Pair<FoundryRawEntry, List<Document>>?> {
                        semaphore.acquire()
                        try {
                            val docs = if (documentBuilder.isJournal(entry)) {
                                documentBuilder.buildJournalDocuments(entry)
                            } else {
                                val metadata = metadataExtractor.extractMetadata(entry.rawJsonContent, entry.foundryType)
                                listOf(documentBuilder.buildDocument(entry, metadata))
                            }
                            if (docs.isNotEmpty()) entry to docs else null
                        } catch (e: Exception) {
                            log.warn("Failed to build document for {}: {}", entry.name, e.message)
                            errors.incrementAndGet()
                            null
                        } finally {
                            semaphore.release()
                        }
                    }
                }

                // Collect results
                val results = futures.mapNotNull { it.get() }

                if (results.isNotEmpty()) {
                    val allDocuments = results.flatMap { it.second }
                    vectorStore.add(allDocuments)
                    processed.addAndGet(results.size)

                    if (markVectorized) {
                        repository.markAsVectorized(results.map { it.first.id })
                    }
                }

                val currentProcessed = processed.get()
                if (currentProcessed % LOG_INTERVAL == 0 || currentProcessed == total) {
                    val elapsed = Duration.between(startTime, Instant.now())
                    val eta = estimateEta(currentProcessed, total, elapsed)
                    log.info("Ingestion progress: {}/{} ({}%) - ETA: {}",
                        currentProcessed, total, (currentProcessed * 100) / maxOf(total, 1), formatDuration(eta))

                    jobId?.let { jobStore.updateProgress(it, currentProcessed, errors.get()) }
                }
            } catch (e: Exception) {
                log.error("Batch ingestion failed: {}", e.message, e)
                errors.addAndGet(batch.size)
            }
        }

        executor.shutdown()

        val duration = Duration.between(startTime, Instant.now())
        log.info("Ingestion completed: {} processed, {} errors in {}",
            processed.get(), errors.get(), formatDuration(duration))

        return IngestionResult(
            processed = processed.get(),
            errors = errors.get(),
            total = total,
            durationSeconds = duration.seconds
        )
    }

    private fun estimateEta(processed: Int, total: Int, elapsed: Duration): Duration {
        if (processed == 0) return Duration.ZERO
        val avgPerItem = elapsed.toMillis() / processed.toDouble()
        val remaining = total - processed
        return Duration.ofMillis((avgPerItem * remaining).toLong())
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

data class IngestionResult(
    val processed: Int,
    val errors: Int,
    val total: Int,
    val durationSeconds: Long
)
