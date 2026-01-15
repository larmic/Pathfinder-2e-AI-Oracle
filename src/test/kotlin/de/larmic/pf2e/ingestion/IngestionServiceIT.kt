package de.larmic.pf2e.ingestion

import de.larmic.pf2e.OllamaTestcontainersConfig
import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

/**
 * Integration test for IngestionService with real Ollama embeddings.
 *
 * This test requires Docker and will pull the nomic-embed-text model (~137MB)
 * on first run, which can take 1-2 minutes.
 *
 * Run with: ./mvnw verify (runs after unit tests)
 * Skip with: ./mvnw test (only unit tests)
 */
@SpringBootTest
@Import(OllamaTestcontainersConfig::class)
@ActiveProfiles("integration")
class IngestionServiceIT {

    @Autowired
    private lateinit var ingestionService: IngestionService

    @Autowired
    private lateinit var repository: FoundryRawEntryRepository

    @Autowired
    private lateinit var vectorStore: VectorStore

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `ingests spell and enables similarity search`() {
        // Given: A spell entry in the database
        val fireballJson = """
            {
                "_id": "fireball-test-id",
                "name": "Fireball",
                "type": "spell",
                "system": {
                    "level": {"value": 3},
                    "traits": {
                        "value": ["concentrate", "fire", "manipulate"],
                        "rarity": "common",
                        "traditions": ["arcane", "primal"]
                    },
                    "description": {
                        "value": "<p>A roaring blast of fire detonates at a spot you designate, dealing 6d6 fire damage.</p>"
                    },
                    "publication": {"title": "Pathfinder Player Core"}
                }
            }
        """.trimIndent()

        val entry = repository.save(
            FoundryRawEntry(
                id = UUID.randomUUID(),
                foundryId = "fireball-test-id",
                foundryType = "spell",
                name = "Fireball",
                rawJsonContent = fireballJson,
                githubSha = "test-sha",
                githubPath = "packs/pf2e/spells/fireball.json"
            )
        )

        // When: We ingest the entry
        val document = ingestionService.ingestEntry(entry)

        // Then: The document is created with correct content
        assertThat(document.text).contains("Fireball")
        assertThat(document.text).contains("fire damage")
        assertThat(document.metadata["foundryType"]).isEqualTo("spell")
        assertThat(document.metadata["level"]).isEqualTo(3)

        // And: We can search for it via similarity
        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("fire damage spell")
                .topK(5)
                .build()
        )

        assertThat(results).isNotEmpty()
        assertThat(results.first().text).contains("Fireball")
    }

    @Test
    fun `ingests multiple entries by type`() {
        // Given: Multiple spell entries
        val spells = listOf(
            createSpellEntry("Magic Missile", "A dart of magical energy"),
            createSpellEntry("Heal", "Positive energy heals the living")
        )
        spells.forEach { repository.save(it) }

        // When: We ingest all spells
        val result = ingestionService.ingestByType("spell")

        // Then: All entries are processed
        assertThat(result.processed).isEqualTo(2)
        assertThat(result.errors).isEqualTo(0)
    }

    private fun createSpellEntry(name: String, description: String): FoundryRawEntry {
        val json = """
            {
                "_id": "${name.lowercase().replace(" ", "-")}-id",
                "name": "$name",
                "type": "spell",
                "system": {
                    "level": {"value": 1},
                    "traits": {"value": [], "rarity": "common"},
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = "${name.lowercase().replace(" ", "-")}-id",
            foundryType = "spell",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/spells/${name.lowercase().replace(" ", "-")}.json"
        )
    }
}
