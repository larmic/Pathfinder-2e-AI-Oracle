package de.larmic.pf2e.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PathfinderItemStoreTest {

    private lateinit var repository: PathfinderItemRepository
    private lateinit var store: PathfinderItemStore

    @BeforeEach
    fun setUp() {
        repository = mockk()
        store = PathfinderItemStore(repository)
    }

    @Test
    fun `save delegates to repository`() {
        val item = createItem()
        every { repository.save(item) } returns item

        val result = store.save(item)

        assertThat(result).isEqualTo(item)
        verify { repository.save(item) }
    }

    @Test
    fun `findByGithubPath delegates to repository`() {
        val item = createItem()
        every { repository.findByGithubPath("path.json") } returns item

        val result = store.findByGithubPath("path.json")

        assertThat(result).isEqualTo(item)
        verify { repository.findByGithubPath("path.json") }
    }

    @Test
    fun `findByFoundryId delegates to repository`() {
        val item = createItem()
        every { repository.findByFoundryId("id123") } returns item

        val result = store.findByFoundryId("id123")

        assertThat(result).isEqualTo(item)
        verify { repository.findByFoundryId("id123") }
    }

    @Test
    fun `findAllByType delegates to repository`() {
        val items = listOf(createItem())
        every { repository.findAllByItemType(ItemType.FEAT) } returns items

        val result = store.findAllByType(ItemType.FEAT)

        assertThat(result).isEqualTo(items)
        verify { repository.findAllByItemType(ItemType.FEAT) }
    }

    @Test
    fun `findAll delegates to repository`() {
        val items = listOf(createItem())
        every { repository.findAll() } returns items

        val result = store.findAll()

        assertThat(result).isEqualTo(items)
        verify { repository.findAll() }
    }

    @Test
    fun `count delegates to repository`() {
        every { repository.count() } returns 42L

        val result = store.count()

        assertThat(result).isEqualTo(42)
        verify { repository.count() }
    }

    @Test
    fun `countByType delegates to repository`() {
        every { repository.countByItemType(ItemType.SPELL) } returns 10L

        val result = store.countByType(ItemType.SPELL)

        assertThat(result).isEqualTo(10)
        verify { repository.countByItemType(ItemType.SPELL) }
    }

    @Test
    fun `clear delegates to repository`() {
        every { repository.deleteAll() } returns Unit

        store.clear()

        verify { repository.deleteAll() }
    }

    private fun createItem() = PathfinderItem(
        foundryId = "foundry-123",
        itemType = ItemType.FEAT,
        itemName = "Test Item",
        rawJsonContent = """{"name": "Test"}""",
        githubSha = "sha-abc",
        githubPath = "test.json"
    )
}
