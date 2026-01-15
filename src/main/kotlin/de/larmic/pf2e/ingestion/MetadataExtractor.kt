package de.larmic.pf2e.ingestion

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * Extracts metadata from raw Foundry JSON content for use in vector store filtering.
 *
 * Metadata fields enable hard-filtering in RAG queries, e.g.:
 * - "Show only spells with Fire trait"
 * - "Feats level 4 or higher"
 * - "Arcane tradition spells only"
 */
@Component
class MetadataExtractor(
    private val objectMapper: ObjectMapper
) {

    /**
     * Extracts metadata from raw JSON content.
     *
     * @param rawJson The raw JSON content from Foundry
     * @param foundryType The type of the entry (spell, feat, action, etc.)
     * @return Map of metadata key-value pairs for vector store
     */
    fun extractMetadata(rawJson: String, foundryType: String): Map<String, Any> {
        val node = objectMapper.readTree(rawJson)
        return buildMap {
            // Always include type and identifiers
            put("foundryType", foundryType)
            put("foundryId", node.path("_id").asText(""))
            put("name", node.path("name").asText(""))

            // Level (for spells, feats, etc.)
            extractLevel(node)?.let { put("level", it) }

            // Traits (comma-separated for filtering)
            extractTraits(node)?.let { put("traits", it) }

            // Rarity (common, uncommon, rare, unique)
            extractRarity(node)?.let { put("rarity", it) }

            // Traditions (for spells: arcane, divine, occult, primal)
            extractTraditions(node)?.let { put("traditions", it) }

            // Source publication
            extractSource(node)?.let { put("source", it) }
        }
    }

    private fun extractLevel(node: JsonNode): Int? {
        val level = node.path("system").path("level").path("value").asInt(-1)
        return if (level >= 0) level else null
    }

    private fun extractTraits(node: JsonNode): String? {
        val traitsNode = node.path("system").path("traits").path("value")
        if (!traitsNode.isArray || traitsNode.isEmpty) return null

        return traitsNode
            .map { it.asText() }
            .filter { it.isNotBlank() }
            .joinToString(",")
            .takeIf { it.isNotBlank() }
    }

    private fun extractRarity(node: JsonNode): String? {
        return node.path("system").path("traits").path("rarity").asText()
            .takeIf { it.isNotBlank() }
    }

    private fun extractTraditions(node: JsonNode): String? {
        val traditionsNode = node.path("system").path("traits").path("traditions")
        if (!traditionsNode.isArray || traditionsNode.isEmpty) return null

        return traditionsNode
            .map { it.asText() }
            .filter { it.isNotBlank() }
            .joinToString(",")
            .takeIf { it.isNotBlank() }
    }

    private fun extractSource(node: JsonNode): String? {
        return node.path("system").path("publication").path("title").asText()
            .takeIf { it.isNotBlank() }
    }
}
