package de.larmic.pf2e.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class PathfinderItemRepositoryIT {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withDatabaseName("pf2e_oracle")
            .withUsername("pf2e")
            .withPassword("pf2e")
    }

    @Autowired
    lateinit var repository: PathfinderItemRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Nested
    inner class Save {

        @Test
        fun `saves and retrieves item`() {
            val item = createItem(itemName = "Test Feat")

            val saved = repository.save(item)

            assertThat(saved.id).isEqualTo(item.id)
            assertThat(repository.count()).isEqualTo(1)
        }

        @Test
        fun `updates item with same id`() {
            val item = createItem(itemName = "Original")
            repository.save(item)

            val updated = item.copy(itemName = "Updated")
            repository.save(updated)

            assertThat(repository.count()).isEqualTo(1)
            assertThat(repository.findById(item.id).get().itemName).isEqualTo("Updated")
        }
    }

    @Nested
    inner class FindByGithubPath {

        @Test
        fun `finds item by github path`() {
            val item = createItem(githubPath = "packs/pf2e/feats/test.json")
            repository.save(item)

            val found = repository.findByGithubPath("packs/pf2e/feats/test.json")

            assertThat(found).isNotNull
            assertThat(found?.githubPath).isEqualTo("packs/pf2e/feats/test.json")
        }

        @Test
        fun `returns null when not found`() {
            val found = repository.findByGithubPath("nonexistent.json")

            assertThat(found).isNull()
        }
    }

    @Nested
    inner class FindByFoundryId {

        @Test
        fun `finds item by foundry id`() {
            val item = createItem(foundryId = "abc123")
            repository.save(item)

            val found = repository.findByFoundryId("abc123")

            assertThat(found).isNotNull
            assertThat(found?.foundryId).isEqualTo("abc123")
        }
    }

    @Nested
    inner class FindAllByItemType {

        @Test
        fun `filters items by type`() {
            repository.save(createItem(itemType = ItemType.FEAT, itemName = "Feat 1", githubPath = "feat1.json"))
            repository.save(createItem(itemType = ItemType.FEAT, itemName = "Feat 2", githubPath = "feat2.json"))
            repository.save(createItem(itemType = ItemType.SPELL, itemName = "Spell 1", githubPath = "spell1.json"))

            val feats = repository.findAllByItemType(ItemType.FEAT)
            val spells = repository.findAllByItemType(ItemType.SPELL)

            assertThat(feats).hasSize(2)
            assertThat(spells).hasSize(1)
        }

        @Test
        fun `returns empty list when no items of type`() {
            repository.save(createItem(itemType = ItemType.FEAT))

            val spells = repository.findAllByItemType(ItemType.SPELL)

            assertThat(spells).isEmpty()
        }
    }

    @Nested
    inner class CountByItemType {

        @Test
        fun `counts items by type`() {
            repository.save(createItem(itemType = ItemType.FEAT, foundryId = "1", githubPath = "f1.json"))
            repository.save(createItem(itemType = ItemType.FEAT, foundryId = "2", githubPath = "f2.json"))
            repository.save(createItem(itemType = ItemType.SPELL, foundryId = "3", githubPath = "s1.json"))

            assertThat(repository.countByItemType(ItemType.FEAT)).isEqualTo(2)
            assertThat(repository.countByItemType(ItemType.SPELL)).isEqualTo(1)
            assertThat(repository.countByItemType(ItemType.ITEM)).isEqualTo(0)
        }
    }

    private fun createItem(
        foundryId: String = "foundry-123",
        itemType: ItemType = ItemType.FEAT,
        itemName: String = "Test Item",
        githubPath: String = "packs/pf2e/feats/test.json",
        githubSha: String = "sha-abc"
    ) = PathfinderItem(
        foundryId = foundryId,
        itemType = itemType,
        itemName = itemName,
        rawJsonContent = """{"name": "$itemName"}""",
        githubSha = githubSha,
        githubPath = githubPath
    )
}
