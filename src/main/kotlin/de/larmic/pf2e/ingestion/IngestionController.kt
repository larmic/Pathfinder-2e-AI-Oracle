package de.larmic.pf2e.ingestion

import de.larmic.pf2e.domain.FoundryRawEntryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for triggering vector store ingestion.
 *
 * Provides endpoints to ingest Foundry entries into the vector store
 * for RAG-based similarity search.
 */
@RestController
@RequestMapping("/api/ingestion")
class IngestionController(
    private val ingestionService: IngestionService,
    private val repository: FoundryRawEntryRepository
) {

    /**
     * Ingest all entries of a specific type.
     *
     * Example: POST /api/ingestion/type/spell
     */
    @PostMapping("/type/{foundryType}")
    fun ingestByType(@PathVariable foundryType: String): ResponseEntity<IngestionResult> {
        val result = ingestionService.ingestByType(foundryType)
        return ResponseEntity.ok(result)
    }

    /**
     * Ingest all entries (full re-index).
     * Warning: This may take 15-30 minutes with Ollama embeddings.
     *
     * Example: POST /api/ingestion/all
     */
    @PostMapping("/all")
    fun ingestAll(): ResponseEntity<IngestionResult> {
        val result = ingestionService.ingestAll()
        return ResponseEntity.ok(result)
    }

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
}

data class TypeCount(
    val type: String,
    val count: Long
)
