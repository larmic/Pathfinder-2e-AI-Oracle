package de.larmic.pf2e.rag

import de.larmic.pf2e.OllamaTestcontainersConfig
import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.ingestion.IngestionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

/**
 * Integration tests for RagService with real vector store and embeddings.
 *
 * These tests verify that the MCP tool methods correctly search
 * the vector store with various filters.
 */
@SpringBootTest
@Import(OllamaTestcontainersConfig::class)
@ActiveProfiles("integration")
class RagServiceIT {

    @Autowired
    private lateinit var ragService: RagService

    @Autowired
    private lateinit var ingestionService: IngestionService

    @Autowired
    private lateinit var repository: FoundryRawEntryRepository

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `searchSpells finds spells by query`() {
        // Given: A spell in the vector store
        val fireball = createSpellEntry(
            name = "Fireball",
            level = 3,
            traits = listOf("concentrate", "fire", "manipulate"),
            traditions = listOf("arcane", "primal"),
            description = "A roaring blast of fire detonates at a spot you designate, dealing 6d6 fire damage."
        )
        repository.save(fireball)
        ingestionService.ingestEntry(fireball)

        // When: We search for fire spells
        val results = ragService.searchSpells(
            query = "fire damage spell",
            level = null,
            maxResults = 5
        )

        // Then: Fireball is found
        assertThat(results.results).isNotEmpty()
        assertThat(results.results.first().name).isEqualTo("Fireball")
        assertThat(results.results.first().type).isEqualTo("spell")
    }

    @Test
    fun `searchSpells filters by level`() {
        // Given: Spells of different levels
        val fireball = createSpellEntry("Fireball", level = 3, description = "Fire damage spell")
        val magicMissile = createSpellEntry("Magic Missile", level = 1, description = "Force damage spell")
        repository.saveAll(listOf(fireball, magicMissile))
        ingestionService.ingestEntry(fireball)
        ingestionService.ingestEntry(magicMissile)

        // When: We search for level 1 spells
        val results = ragService.searchSpells(
            query = "damage spell",
            level = 1,
            maxResults = 5
        )

        // Then: Only Magic Missile is found
        assertThat(results.results).hasSize(1)
        assertThat(results.results.first().name).isEqualTo("Magic Missile")
    }

    @Test
    fun `searchSpells finds spells by tradition in query`() {
        // Given: Spells with different traditions
        val fireball = createSpellEntry(
            name = "Fireball",
            traditions = listOf("arcane", "primal"),
            description = "Arcane fire spell explosion"
        )
        val heal = createSpellEntry(
            name = "Heal",
            traditions = listOf("divine", "primal"),
            description = "Divine healing spell that restores health"
        )
        repository.saveAll(listOf(fireball, heal))
        ingestionService.ingestEntry(fireball)
        ingestionService.ingestEntry(heal)

        // When: We search for divine healing spells (using semantic search)
        val results = ragService.searchSpells(
            query = "divine healing spell",
            level = null,
            maxResults = 5
        )

        // Then: Heal should be ranked higher due to semantic similarity
        assertThat(results.results).isNotEmpty()
        assertThat(results.results.first().name).isEqualTo("Heal")
    }

    @Test
    fun `searchFeats finds feats with maxLevel filter`() {
        // Given: Feats of different levels
        val lowLevelFeat = createFeatEntry("Quick Jump", level = 1, description = "Jump quickly")
        val highLevelFeat = createFeatEntry("Legendary Jumper", level = 15, description = "Jump far")
        repository.saveAll(listOf(lowLevelFeat, highLevelFeat))
        ingestionService.ingestEntry(lowLevelFeat)
        ingestionService.ingestEntry(highLevelFeat)

        // When: We search for feats up to level 5
        val results = ragService.searchFeats(
            query = "jump",
            maxLevel = 5,
            maxResults = 5
        )

        // Then: Only the low-level feat is found
        assertThat(results.results).hasSize(1)
        assertThat(results.results.first().name).isEqualTo("Quick Jump")
    }

    @Test
    fun `searchActions finds action entries`() {
        // Given: An action in the vector store
        val strike = createActionEntry("Strike", "Make a melee or ranged attack")
        repository.save(strike)
        ingestionService.ingestEntry(strike)

        // When: We search for actions
        val results = ragService.searchActions(
            query = "attack",
            maxResults = 5
        )

        // Then: Strike is found
        assertThat(results.results).isNotEmpty()
        assertThat(results.results.first().name).isEqualTo("Strike")
        assertThat(results.results.first().type).isEqualTo("action")
    }

    @Test
    fun `searchEquipment filters by rarity`() {
        // Given: Equipment with different rarities
        val commonSword = createEquipmentEntry("Longsword", rarity = "common", description = "A common blade")
        val rareBlade = createEquipmentEntry("Vorpal Blade", rarity = "rare", description = "A legendary blade")
        repository.saveAll(listOf(commonSword, rareBlade))
        ingestionService.ingestEntry(commonSword)
        ingestionService.ingestEntry(rareBlade)

        // When: We search for rare equipment
        val results = ragService.searchEquipment(
            query = "blade",
            maxLevel = null,
            rarity = "rare",
            maxResults = 5
        )

        // Then: Only the rare item is found
        assertThat(results.results).hasSize(1)
        assertThat(results.results.first().name).isEqualTo("Vorpal Blade")
    }

    @Test
    fun `searchConditions finds condition entries`() {
        // Given: A condition in the vector store
        val frightened = createConditionEntry("Frightened", "You're gripped by fear")
        repository.save(frightened)
        ingestionService.ingestEntry(frightened)

        // When: We search for conditions
        val results = ragService.searchConditions(
            query = "fear",
            maxResults = 5
        )

        // Then: Frightened is found
        assertThat(results.results).isNotEmpty()
        assertThat(results.results.first().name).isEqualTo("Frightened")
        assertThat(results.results.first().type).isEqualTo("condition")
    }

    @Test
    fun `getEntry finds exact entry by name`() {
        // Given: Multiple spells including Fireball
        val fireball = createSpellEntry("Fireball", description = "Fire explosion spell")
        val fireShield = createSpellEntry("Fire Shield", description = "Fire protection spell")
        repository.saveAll(listOf(fireball, fireShield))
        ingestionService.ingestEntry(fireball)
        ingestionService.ingestEntry(fireShield)

        // When: We get entry by exact name
        val results = ragService.getEntry(name = "Fireball", type = "spell")

        // Then: Only Fireball is returned
        assertThat(results.results).hasSize(1)
        assertThat(results.results.first().name).isEqualTo("Fireball")
    }

    @Test
    fun `searchRules finds general rule content`() {
        // Given: Various rule entries
        val action = createActionEntry("Stride", "Move up to your Speed")
        repository.save(action)
        ingestionService.ingestEntry(action)

        // When: We search for movement rules
        val results = ragService.searchRules(
            query = "movement speed",
            maxResults = 5
        )

        // Then: Stride action is found
        assertThat(results.results).isNotEmpty()
        assertThat(results.results.first().name).isEqualTo("Stride")
    }

    // Helper methods to create test entries

    private fun createSpellEntry(
        name: String,
        level: Int = 1,
        traits: List<String> = emptyList(),
        traditions: List<String> = listOf("arcane"),
        description: String = "A spell"
    ): FoundryRawEntry {
        val id = name.lowercase().replace(" ", "-")
        val traitsJson = traits.joinToString("\", \"", "[\"", "\"]").takeIf { traits.isNotEmpty() } ?: "[]"
        val traditionsJson = traditions.joinToString("\", \"", "[\"", "\"]")

        val json = """
            {
                "_id": "$id",
                "name": "$name",
                "type": "spell",
                "system": {
                    "level": {"value": $level},
                    "traits": {
                        "value": $traitsJson,
                        "rarity": "common",
                        "traditions": $traditionsJson
                    },
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = id,
            foundryType = "spell",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/spells/$id.json"
        )
    }

    private fun createFeatEntry(
        name: String,
        level: Int = 1,
        traits: List<String> = listOf("general"),
        description: String = "A feat"
    ): FoundryRawEntry {
        val id = name.lowercase().replace(" ", "-")
        val traitsJson = traits.joinToString("\", \"", "[\"", "\"]")

        val json = """
            {
                "_id": "$id",
                "name": "$name",
                "type": "feat",
                "system": {
                    "level": {"value": $level},
                    "traits": {
                        "value": $traitsJson,
                        "rarity": "common"
                    },
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = id,
            foundryType = "feat",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/feats/$id.json"
        )
    }

    private fun createActionEntry(
        name: String,
        description: String = "An action"
    ): FoundryRawEntry {
        val id = name.lowercase().replace(" ", "-")

        val json = """
            {
                "_id": "$id",
                "name": "$name",
                "type": "action",
                "system": {
                    "traits": {"value": [], "rarity": "common"},
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = id,
            foundryType = "action",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/actions/$id.json"
        )
    }

    private fun createEquipmentEntry(
        name: String,
        level: Int = 0,
        rarity: String = "common",
        description: String = "An item"
    ): FoundryRawEntry {
        val id = name.lowercase().replace(" ", "-")

        val json = """
            {
                "_id": "$id",
                "name": "$name",
                "type": "equipment",
                "system": {
                    "level": {"value": $level},
                    "traits": {"value": [], "rarity": "$rarity"},
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = id,
            foundryType = "equipment",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/equipment/$id.json"
        )
    }

    private fun createConditionEntry(
        name: String,
        description: String = "A condition"
    ): FoundryRawEntry {
        val id = name.lowercase().replace(" ", "-")

        val json = """
            {
                "_id": "$id",
                "name": "$name",
                "type": "condition",
                "system": {
                    "traits": {"value": [], "rarity": "common"},
                    "description": {"value": "<p>$description</p>"},
                    "publication": {"title": "Test Source"}
                }
            }
        """.trimIndent()

        return FoundryRawEntry(
            id = UUID.randomUUID(),
            foundryId = id,
            foundryType = "condition",
            name = name,
            rawJsonContent = json,
            githubSha = "test-sha",
            githubPath = "packs/pf2e/conditions/$id.json"
        )
    }
}
