package de.larmic.pf2e.rag

/**
 * Represents a single search result from the RAG vector store.
 */
data class RagResult(
    val name: String,
    val type: String,
    val level: Int?,
    val traits: List<String>,
    val content: String,
    val source: String?,
    val similarity: Double
)

/**
 * Container for search results with query context.
 */
data class SearchResults(
    val query: String,
    val resultCount: Int,
    val results: List<RagResult>
)
