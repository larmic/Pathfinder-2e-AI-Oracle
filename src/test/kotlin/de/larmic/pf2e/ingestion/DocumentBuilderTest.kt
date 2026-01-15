package de.larmic.pf2e.ingestion

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.parser.FoundryContentParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class DocumentBuilderTest {

    private val objectMapper = jacksonObjectMapper()
    private val contentParser = FoundryContentParser()
    private val documentBuilder = DocumentBuilder(contentParser, objectMapper)

    private fun createEntry(
        name: String,
        foundryType: String,
        rawJson: String
    ) = FoundryRawEntry(
        id = UUID.randomUUID(),
        foundryId = "test-id",
        foundryType = foundryType,
        name = name,
        rawJsonContent = rawJson,
        githubSha = "test-sha",
        githubPath = "test/path.json"
    )

    @Nested
    inner class BuildDocument {

        @Test
        fun `builds document with all metadata`() {
            val json = """
                {
                    "_id": "sxQZ6yqTn0czJxVd",
                    "name": "Fireball",
                    "type": "spell",
                    "system": {
                        "level": {"value": 3},
                        "traits": {
                            "value": ["concentrate", "fire", "manipulate"],
                            "rarity": "common",
                            "traditions": ["arcane", "primal"]
                        },
                        "description": {"value": "<p>A roaring blast of fire.</p>"}
                    }
                }
            """.trimIndent()

            val entry = createEntry("Fireball", "spell", json)
            val metadata = mapOf(
                "foundryType" to "spell",
                "foundryId" to "sxQZ6yqTn0czJxVd",
                "name" to "Fireball",
                "level" to 3,
                "traits" to "concentrate,fire,manipulate",
                "rarity" to "common",
                "traditions" to "arcane,primal"
            )

            val document = documentBuilder.buildDocument(entry, metadata)

            assertThat(document.text).contains("Name: Fireball")
            assertThat(document.text).contains("Type: Spell (Level 3)")
            assertThat(document.text).contains("Traits: concentrate,fire,manipulate")
            assertThat(document.text).contains("Traditions: arcane,primal")
            assertThat(document.text).contains("A roaring blast of fire.")
            assertThat(document.text).doesNotContain("<p>")
            assertThat(document.metadata).isEqualTo(metadata)
        }

        @Test
        fun `includes uncommon rarity in content`() {
            val json = """
                {
                    "_id": "rare123",
                    "name": "Rare Spell",
                    "system": {
                        "traits": {"rarity": "uncommon"},
                        "description": {"value": "A rare spell."}
                    }
                }
            """.trimIndent()

            val entry = createEntry("Rare Spell", "spell", json)
            val metadata = mapOf(
                "foundryType" to "spell",
                "name" to "Rare Spell",
                "rarity" to "uncommon"
            )

            val document = documentBuilder.buildDocument(entry, metadata)

            assertThat(document.text).contains("Rarity: uncommon")
        }

        @Test
        fun `excludes common rarity from content`() {
            val json = """
                {
                    "_id": "common123",
                    "name": "Common Spell",
                    "system": {
                        "traits": {"rarity": "common"},
                        "description": {"value": "A common spell."}
                    }
                }
            """.trimIndent()

            val entry = createEntry("Common Spell", "spell", json)
            val metadata = mapOf(
                "foundryType" to "spell",
                "name" to "Common Spell",
                "rarity" to "common"
            )

            val document = documentBuilder.buildDocument(entry, metadata)

            assertThat(document.text).doesNotContain("Rarity:")
        }

        @Test
        fun `handles entry without description`() {
            val json = """
                {
                    "_id": "nodesc123",
                    "name": "No Description",
                    "system": {}
                }
            """.trimIndent()

            val entry = createEntry("No Description", "action", json)
            val metadata = mapOf(
                "foundryType" to "action",
                "name" to "No Description"
            )

            val document = documentBuilder.buildDocument(entry, metadata)

            assertThat(document.text).contains("Name: No Description")
            assertThat(document.text).contains("Type: Action")
        }

        @Test
        fun `cleans Foundry tags from description`() {
            val json = """
                {
                    "_id": "tagged123",
                    "name": "Tagged Spell",
                    "system": {
                        "description": {
                            "value": "<p>You become @UUID[Compendium.pf2e.conditionitems.Item.Clumsy]{Clumsy 2}.</p>"
                        }
                    }
                }
            """.trimIndent()

            val entry = createEntry("Tagged Spell", "spell", json)
            val metadata = mapOf("foundryType" to "spell", "name" to "Tagged Spell")

            val document = documentBuilder.buildDocument(entry, metadata)

            assertThat(document.text).contains("You become Clumsy 2.")
            assertThat(document.text).doesNotContain("@UUID")
            assertThat(document.text).doesNotContain("<p>")
        }
    }
}
