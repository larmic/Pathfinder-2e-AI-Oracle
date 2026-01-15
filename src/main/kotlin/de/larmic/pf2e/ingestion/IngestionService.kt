package de.larmic.pf2e.ingestion

import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Service for ingesting Foundry raw entries into the vector store.
 *
 * Converts raw JSON content to embeddings with metadata for RAG-based
 * similarity search. Uses batch processing for efficient handling of
 * large datasets (~28,000 entries).
 */
@Service
class IngestionService(
    private val repository: FoundryRawEntryRepository,
    private val vectorStore: VectorStore,
    private val metadataExtractor: MetadataExtractor,
    private val documentBuilder: DocumentBuilder
) {

    private val log = LoggerFactory.getLogger(IngestionService::class.java)

    companion object {
        private const val BATCH_SIZE = 50
        private const val LOG_INTERVAL = 500
    }

    /**
     * Ingest all entries of a specific type (e.g., "spell", "feat").
     */
    fun ingestByType(foundryType: String): IngestionResult {
        log.info("Starting ingestion for type: {}", foundryType)
        val entries = repository.findAllByFoundryType(foundryType)
        return ingestEntries(entries, "type=$foundryType")
    }

    /**
     * Ingest all entries (full re-index).
     * Warning: This may take 15-30 minutes with Ollama embeddings.
     */
    fun ingestAll(): IngestionResult {
        log.info("Starting full ingestion of all entries")
        val entries = repository.findAll()
        return ingestEntries(entries, "all")
    }

    /**
     * Ingest a single entry by its database entity.
     */
    fun ingestEntry(entry: FoundryRawEntry): Document {
        val metadata = metadataExtractor.extractMetadata(entry.rawJsonContent, entry.foundryType)
        val document = documentBuilder.buildDocument(entry, metadata)
        vectorStore.add(listOf(document))
        log.debug("Ingested single entry: {} ({})", entry.name, entry.foundryType)
        return document
    }

    private fun ingestEntries(entries: List<FoundryRawEntry>, context: String): IngestionResult {
        val startTime = Instant.now()
        var processed = 0
        var errors = 0
        val total = entries.size

        log.info("Found {} entries to ingest ({})", total, context)

        entries.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val documents = batch.mapNotNull { entry ->
                    try {
                        val metadata = metadataExtractor.extractMetadata(entry.rawJsonContent, entry.foundryType)
                        documentBuilder.buildDocument(entry, metadata)
                    } catch (e: Exception) {
                        log.warn("Failed to build document for {}: {}", entry.name, e.message)
                        errors++
                        null
                    }
                }

                if (documents.isNotEmpty()) {
                    vectorStore.add(documents)
                    processed += documents.size
                }

                if (processed % LOG_INTERVAL == 0 || processed == total) {
                    val elapsed = Duration.between(startTime, Instant.now())
                    val eta = estimateEta(processed, total, elapsed)
                    log.info("Ingestion progress: {}/{} ({}%) - ETA: {}",
                        processed, total, (processed * 100) / total, formatDuration(eta))
                }
            } catch (e: Exception) {
                log.error("Batch ingestion failed: {}", e.message, e)
                errors += batch.size
            }
        }

        val duration = Duration.between(startTime, Instant.now())
        log.info("Ingestion completed: {} processed, {} errors in {}",
            processed, errors, formatDuration(duration))

        return IngestionResult(
            processed = processed,
            errors = errors,
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
