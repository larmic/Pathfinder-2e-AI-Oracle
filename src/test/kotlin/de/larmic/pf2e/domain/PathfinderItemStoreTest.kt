package de.larmic.pf2e.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PathfinderItemStoreTest {

    private lateinit var store: PathfinderItemStore

    @BeforeEach
    fun setUp() {
        store = PathfinderItemStore()
    }

    @Nested
    inner class Save {

        @Test
        fun `saves and returns item`() {
            val item = createItem(itemName = "Test Feat")

            val saved = store.save(item)

            assertThat(saved).isEqualTo(item)
            assertThat(store.count()).isEqualTo(1)
        }

        @Test
        fun `overwrites item with same id`() {
            val item1 = createItem(itemName = "Original")
            val item2 = item1.copy(itemName = "Updated")

            store.save(item1)
            store.save(item2)

            assertThat(store.count()).isEqualTo(1)
            assertThat(store.findAll().first().itemName).isEqualTo("Updated")
        }
    }

    @Nested
    inner class FindByGithubPath {

        @Test
        fun `finds item by github path`() {
            val item = createItem(githubPath = "packs/pf2e/feats/test.json")
            store.save(item)

            val found = store.findByGithubPath("packs/pf2e/feats/test.json")

            assertThat(found).isNotNull
            assertThat(found?.githubPath).isEqualTo("packs/pf2e/feats/test.json")
        }

        @Test
        fun `returns null when not found`() {
            val found = store.findByGithubPath("nonexistent.json")

            assertThat(found).isNull()
        }
    }

    @Nested
    inner class FindByFoundryId {

        @Test
        fun `finds item by foundry id`() {
            val item = createItem(foundryId = "abc123")
            store.save(item)

            val found = store.findByFoundryId("abc123")

            assertThat(found).isNotNull
            assertThat(found?.foundryId).isEqualTo("abc123")
        }
    }

    @Nested
    inner class FindAllByType {

        @Test
        fun `filters items by type`() {
            store.save(createItem(itemType = ItemType.FEAT, itemName = "Feat 1"))
            store.save(createItem(itemType = ItemType.FEAT, itemName = "Feat 2"))
            store.save(createItem(itemType = ItemType.SPELL, itemName = "Spell 1"))

            val feats = store.findAllByType(ItemType.FEAT)
            val spells = store.findAllByType(ItemType.SPELL)

            assertThat(feats).hasSize(2)
            assertThat(spells).hasSize(1)
        }

        @Test
        fun `returns empty list when no items of type`() {
            store.save(createItem(itemType = ItemType.FEAT))

            val spells = store.findAllByType(ItemType.SPELL)

            assertThat(spells).isEmpty()
        }
    }

    @Nested
    inner class CountByType {

        @Test
        fun `counts items by type`() {
            store.save(createItem(itemType = ItemType.FEAT, foundryId = "1"))
            store.save(createItem(itemType = ItemType.FEAT, foundryId = "2"))
            store.save(createItem(itemType = ItemType.SPELL, foundryId = "3"))

            assertThat(store.countByType(ItemType.FEAT)).isEqualTo(2)
            assertThat(store.countByType(ItemType.SPELL)).isEqualTo(1)
            assertThat(store.countByType(ItemType.ITEM)).isEqualTo(0)
        }
    }

    @Nested
    inner class Clear {

        @Test
        fun `removes all items`() {
            store.save(createItem(foundryId = "1"))
            store.save(createItem(foundryId = "2"))

            store.clear()

            assertThat(store.count()).isEqualTo(0)
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
