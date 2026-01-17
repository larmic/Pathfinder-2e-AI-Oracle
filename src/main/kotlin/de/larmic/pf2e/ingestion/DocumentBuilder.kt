package de.larmic.pf2e.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.parser.FoundryContentParser
import org.springframework.ai.document.Document
import org.springframework.stereotype.Component

/**
 * Builds Spring AI Document objects from Foundry raw entries.
 *
 * Creates semantic content optimized for embedding generation,
 * combining structured metadata with cleaned description text.
 */
@Component
class DocumentBuilder(
    private val contentParser: FoundryContentParser,
    private val objectMapper: ObjectMapper
) {

    /**
     * Builds a Document from a Foundry entry with its extracted metadata.
     *
     * The document content is formatted for optimal semantic search:
     * - Name and type as header
     * - Level, traits, traditions as structured info
     * - Cleaned description without Foundry-specific markup
     *
     * @param entry The raw Foundry entry from database
     * @param metadata Pre-extracted metadata for filtering
     * @return Spring AI Document ready for embedding
     */
    fun buildDocument(entry: FoundryRawEntry, metadata: Map<String, Any>): Document {
        val node = objectMapper.readTree(entry.rawJsonContent)

        val content = buildString {
            // Header with name and type
            appendLine("Name: ${entry.name}")
            append("Type: ${entry.foundryType.replaceFirstChar { it.uppercase() }}")
            metadata["level"]?.let { append(" (Level $it)") }
            appendLine()

            // Structured metadata
            metadata["traits"]?.let { appendLine("Traits: $it") }
            metadata["traditions"]?.let { appendLine("Traditions: $it") }
            metadata["rarity"]?.let {
                if (it != "common") appendLine("Rarity: $it")
            }

            appendLine()

            // Cleaned description
            val rawDescription = node.path("system").path("description").path("value").asText("")
            if (rawDescription.isNotBlank()) {
                val cleanDescription = contentParser.cleanContent(rawDescription)
                appendLine(cleanDescription)
            }
        }

        return Document(entry.id.toString(), content.trim(), metadata)
    }
}
