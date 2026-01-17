package de.larmic.pf2e.rag

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.stereotype.Service

/**
 * RAG Service providing Pathfinder 2e rule searches via MCP tools.
 *
 * Exposes search functionality for various Foundry content types
 * (spells, feats, actions, equipment, conditions) as MCP tools
 * that can be used by AI assistants like Claude.
 */
@Service
class RagService(
    private val vectorStore: VectorStore
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_TOP_K = 5
    }

    @Tool(description = "Search for general Pathfinder 2e rules, mechanics, and game information. Use this for broad rule questions.")
    fun searchRules(
        @ToolParam(description = "The search query describing what rules you're looking for")
        query: String,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K)
        return toSearchResults(query, results)
    }

    @Tool(description = "Search for Pathfinder 2e spells. Can filter by level. Include tradition (arcane, divine, occult, primal) or traits (fire, healing, mental) in your query for better results.")
    fun searchSpells(
        @ToolParam(description = "The search query describing the spell you're looking for. Include tradition or trait names for better filtering.")
        query: String,
        @ToolParam(description = "Filter by exact spell level (0-10)", required = false)
        level: Int?,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        var filter = filterBuilder.eq("foundryType", "spell")

        level?.let {
            filter = filterBuilder.and(filter, filterBuilder.eq("level", it))
        }

        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K, filter.build())
        return toSearchResults(query, results)
    }

    @Tool(description = "Search for Pathfinder 2e feats. Can filter by maximum level. Include trait names (general, skill, archetype) in your query for better results.")
    fun searchFeats(
        @ToolParam(description = "The search query describing the feat you're looking for. Include trait names for better filtering.")
        query: String,
        @ToolParam(description = "Filter by maximum feat level", required = false)
        maxLevel: Int?,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        var filter = filterBuilder.eq("foundryType", "feat")

        maxLevel?.let {
            filter = filterBuilder.and(filter, filterBuilder.lte("level", it))
        }

        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K, filter.build())
        return toSearchResults(query, results)
    }

    @Tool(description = "Search for Pathfinder 2e actions and activities.")
    fun searchActions(
        @ToolParam(description = "The search query describing the action you're looking for")
        query: String,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        val filter = filterBuilder.eq("foundryType", "action")

        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K, filter.build())
        return toSearchResults(query, results)
    }

    @Tool(description = "Search for Pathfinder 2e equipment, items, and gear. Can filter by maximum level and rarity.")
    fun searchEquipment(
        @ToolParam(description = "The search query describing the equipment you're looking for")
        query: String,
        @ToolParam(description = "Filter by maximum item level", required = false)
        maxLevel: Int?,
        @ToolParam(description = "Filter by rarity: common, uncommon, rare, or unique", required = false)
        rarity: String?,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        var filter = filterBuilder.eq("foundryType", "equipment")

        maxLevel?.let {
            filter = filterBuilder.and(filter, filterBuilder.lte("level", it))
        }

        rarity?.let { r ->
            filter = filterBuilder.and(filter, filterBuilder.eq("rarity", r.lowercase()))
        }

        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K, filter.build())
        return toSearchResults(query, results)
    }

    @Tool(description = "Search for Pathfinder 2e conditions and status effects.")
    fun searchConditions(
        @ToolParam(description = "The search query describing the condition you're looking for")
        query: String,
        @ToolParam(description = "Maximum number of results to return (default: 5)", required = false)
        maxResults: Int?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        val filter = filterBuilder.eq("foundryType", "condition")

        val results = executeSearch(query, maxResults ?: DEFAULT_TOP_K, filter.build())
        return toSearchResults(query, results)
    }

    @Tool(description = "Get a specific Pathfinder 2e entry by its exact name. Use this when you know the exact name of what you're looking for.")
    fun getEntry(
        @ToolParam(description = "The exact name of the entry (e.g., 'Fireball', 'Power Attack')")
        name: String,
        @ToolParam(description = "The type of entry: spell, feat, action, equipment, condition, or ancestry", required = false)
        type: String?
    ): SearchResults {
        val filterBuilder = FilterExpressionBuilder()
        var filter = filterBuilder.eq("name", name)

        type?.let { t ->
            filter = filterBuilder.and(filter, filterBuilder.eq("foundryType", t.lowercase()))
        }

        val results = executeSearch(name, 1, filter.build())
        return toSearchResults(name, results)
    }

    private fun executeSearch(
        query: String,
        topK: Int,
        filterExpression: org.springframework.ai.vectorstore.filter.Filter.Expression? = null
    ): List<Document> {
        log.info("RAG search: query='{}', topK={}, filter={}", query.take(50), topK, filterExpression)
        val startTime = System.currentTimeMillis()

        val requestBuilder = SearchRequest.builder()
            .query(query)
            .topK(topK)

        filterExpression?.let {
            requestBuilder.filterExpression(it)
        }

        val results = vectorStore.similaritySearch(requestBuilder.build())

        val duration = System.currentTimeMillis() - startTime
        log.info("RAG search completed in {}ms, results: {}", duration, results.size)

        return results
    }

    private fun toSearchResults(query: String, documents: List<Document>): SearchResults {
        val results = documents.map { doc ->
            RagResult(
                name = doc.metadata["name"]?.toString() ?: "Unknown",
                type = doc.metadata["foundryType"]?.toString() ?: "Unknown",
                level = (doc.metadata["level"] as? Number)?.toInt(),
                traits = parseTraits(doc.metadata["traits"]?.toString()),
                content = doc.text ?: "",
                source = doc.metadata["source"]?.toString(),
                similarity = doc.score ?: 0.0
            )
        }

        return SearchResults(
            query = query,
            resultCount = results.size,
            results = results
        )
    }

    private fun parseTraits(traits: String?): List<String> {
        return traits?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }
}
