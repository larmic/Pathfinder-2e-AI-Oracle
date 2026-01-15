package de.larmic.pf2e.ingestion

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MetadataExtractorTest {

    private val objectMapper = jacksonObjectMapper()
    private val extractor = MetadataExtractor(objectMapper)

    @Nested
    inner class ExtractMetadata {

        @Test
        fun `extracts all metadata from spell JSON`() {
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
                        "publication": {"title": "Pathfinder Player Core"}
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "spell")

            assertThat(metadata["foundryType"]).isEqualTo("spell")
            assertThat(metadata["foundryId"]).isEqualTo("sxQZ6yqTn0czJxVd")
            assertThat(metadata["name"]).isEqualTo("Fireball")
            assertThat(metadata["level"]).isEqualTo(3)
            assertThat(metadata["traits"]).isEqualTo("concentrate,fire,manipulate")
            assertThat(metadata["rarity"]).isEqualTo("common")
            assertThat(metadata["traditions"]).isEqualTo("arcane,primal")
            assertThat(metadata["source"]).isEqualTo("Pathfinder Player Core")
        }

        @Test
        fun `extracts metadata from feat without traditions`() {
            val json = """
                {
                    "_id": "feat123",
                    "name": "Power Attack",
                    "type": "feat",
                    "system": {
                        "level": {"value": 1},
                        "traits": {
                            "value": ["fighter"],
                            "rarity": "common"
                        },
                        "publication": {"title": "Pathfinder Core Rulebook"}
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "feat")

            assertThat(metadata["foundryType"]).isEqualTo("feat")
            assertThat(metadata["foundryId"]).isEqualTo("feat123")
            assertThat(metadata["name"]).isEqualTo("Power Attack")
            assertThat(metadata["level"]).isEqualTo(1)
            assertThat(metadata["traits"]).isEqualTo("fighter")
            assertThat(metadata["rarity"]).isEqualTo("common")
            assertThat(metadata).doesNotContainKey("traditions")
            assertThat(metadata["source"]).isEqualTo("Pathfinder Core Rulebook")
        }

        @Test
        fun `handles missing optional fields gracefully`() {
            val json = """
                {
                    "_id": "action123",
                    "name": "Strike",
                    "type": "action",
                    "system": {}
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "action")

            assertThat(metadata["foundryType"]).isEqualTo("action")
            assertThat(metadata["foundryId"]).isEqualTo("action123")
            assertThat(metadata["name"]).isEqualTo("Strike")
            assertThat(metadata).doesNotContainKey("level")
            assertThat(metadata).doesNotContainKey("traits")
            assertThat(metadata).doesNotContainKey("rarity")
            assertThat(metadata).doesNotContainKey("traditions")
            assertThat(metadata).doesNotContainKey("source")
        }

        @Test
        fun `handles empty traits array`() {
            val json = """
                {
                    "_id": "item123",
                    "name": "Sword",
                    "type": "equipment",
                    "system": {
                        "traits": {"value": []}
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "equipment")

            assertThat(metadata).doesNotContainKey("traits")
        }

        @Test
        fun `handles level 0 correctly`() {
            val json = """
                {
                    "_id": "cantrip123",
                    "name": "Light",
                    "type": "spell",
                    "system": {
                        "level": {"value": 0}
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "spell")

            assertThat(metadata["level"]).isEqualTo(0)
        }

        @Test
        fun `extracts uncommon rarity`() {
            val json = """
                {
                    "_id": "rare123",
                    "name": "Rare Spell",
                    "type": "spell",
                    "system": {
                        "traits": {
                            "rarity": "uncommon"
                        }
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "spell")

            assertThat(metadata["rarity"]).isEqualTo("uncommon")
        }

        @Test
        fun `handles multiple traditions`() {
            val json = """
                {
                    "_id": "spell123",
                    "name": "Heal",
                    "type": "spell",
                    "system": {
                        "traits": {
                            "traditions": ["divine", "primal"]
                        }
                    }
                }
            """.trimIndent()

            val metadata = extractor.extractMetadata(json, "spell")

            assertThat(metadata["traditions"]).isEqualTo("divine,primal")
        }
    }
}
