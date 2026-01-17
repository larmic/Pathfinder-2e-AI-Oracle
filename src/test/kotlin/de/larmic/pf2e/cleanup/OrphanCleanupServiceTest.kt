package de.larmic.pf2e.cleanup

import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.domain.IdAndPath
import de.larmic.pf2e.github.GitHubClient
import de.larmic.pf2e.github.GitHubTreeEntry
import de.larmic.pf2e.github.GitHubTreeResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import java.util.*

class OrphanCleanupServiceTest {

    private val gitHubClient: GitHubClient = mockk(relaxed = true)
    private val repository: FoundryRawEntryRepository = mockk()
    private val vectorStore: VectorStore = mockk()

    private lateinit var service: OrphanCleanupService

    @BeforeEach
    fun setUp() {
        service = OrphanCleanupService(gitHubClient, repository, vectorStore)
    }

    @Nested
    inner class DetectOrphans {

        @Test
        fun `returns empty list when all entries exist in GitHub`() {
            val githubPaths = listOf(
                createTreeEntry("packs/pf2e/feats/feat1.json"),
                createTreeEntry("packs/pf2e/feats/feat2.json")
            )
            val storedEntries = listOf(
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/feat1.json"),
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/feat2.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries

            val orphans = service.detectOrphans()

            assertThat(orphans).isEmpty()
        }

        @Test
        fun `detects orphan when entry not in GitHub`() {
            val id = UUID.randomUUID()
            val githubPaths = listOf(
                createTreeEntry("packs/pf2e/feats/feat1.json")
            )
            val storedEntries = listOf(
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/feat1.json"),
                createIdAndPath(id, "packs/pf2e/feats/deleted-feat.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries

            val orphans = service.detectOrphans()

            assertThat(orphans).hasSize(1)
            assertThat(orphans[0].id).isEqualTo(id)
            assertThat(orphans[0].githubPath).isEqualTo("packs/pf2e/feats/deleted-feat.json")
            assertThat(orphans[0].category).isEqualTo("feats")
        }

        @Test
        fun `detects multiple orphans across categories`() {
            val githubPaths = listOf(
                createTreeEntry("packs/pf2e/feats/kept.json")
            )
            val storedEntries = listOf(
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/kept.json"),
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/deleted-feat.json"),
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/spells/deleted-spell.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries

            val orphans = service.detectOrphans()

            assertThat(orphans).hasSize(2)
            assertThat(orphans.map { it.category }).containsExactlyInAnyOrder("feats", "spells")
        }
    }

    @Nested
    inner class CleanupOrphans {

        @Test
        fun `returns zero counts when no orphans`() {
            val githubPaths = listOf(
                createTreeEntry("packs/pf2e/feats/feat1.json")
            )
            val storedEntries = listOf(
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/feat1.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries

            val result = service.cleanupOrphans()

            assertThat(result.deletedFromDatabase).isEqualTo(0)
            assertThat(result.deletedFromVectorStore).isEqualTo(0)
            assertThat(result.orphanPaths).isEmpty()

            verify(exactly = 0) { vectorStore.delete(any<List<String>>()) }
            verify(exactly = 0) { repository.deleteAllByIds(any()) }
        }

        @Test
        fun `deletes orphans from database and vector store`() {
            val orphanId = UUID.randomUUID()
            val githubPaths = listOf(
                createTreeEntry("packs/pf2e/feats/kept.json")
            )
            val storedEntries = listOf(
                createIdAndPath(UUID.randomUUID(), "packs/pf2e/feats/kept.json"),
                createIdAndPath(orphanId, "packs/pf2e/feats/deleted.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries
            every { vectorStore.delete(any<List<String>>()) } returns Unit
            every { repository.deleteAllByIds(any()) } returns Unit

            val result = service.cleanupOrphans()

            assertThat(result.deletedFromDatabase).isEqualTo(1)
            assertThat(result.deletedFromVectorStore).isEqualTo(1)
            assertThat(result.orphanPaths).containsExactly("packs/pf2e/feats/deleted.json")

            verify { vectorStore.delete(listOf(orphanId.toString())) }
            verify { repository.deleteAllByIds(listOf(orphanId)) }
        }

        @Test
        fun `continues database deletion when vector store fails`() {
            val orphanId = UUID.randomUUID()
            val githubPaths = emptyList<GitHubTreeEntry>()
            val storedEntries = listOf(
                createIdAndPath(orphanId, "packs/pf2e/feats/orphan.json")
            )

            every { gitHubClient.getTree() } returns createTreeResponse(githubPaths)
            every { gitHubClient.filterTreeEntries(any(), "packs/pf2e") } returns githubPaths
            every { repository.findAllIdAndPaths() } returns storedEntries
            every { vectorStore.delete(any<List<String>>()) } throws RuntimeException("Vector store error")
            every { repository.deleteAllByIds(any()) } returns Unit

            val result = service.cleanupOrphans()

            assertThat(result.deletedFromDatabase).isEqualTo(1)
            assertThat(result.deletedFromVectorStore).isEqualTo(0)

            verify { repository.deleteAllByIds(listOf(orphanId)) }
        }
    }

    @Nested
    inner class OrphanInfoFromIdAndPath {

        @Test
        fun `extracts category from path`() {
            val id = UUID.randomUUID()

            val info = OrphanInfo.fromIdAndPath(id, "packs/pf2e/feats/some-feat.json")

            assertThat(info.id).isEqualTo(id)
            assertThat(info.githubPath).isEqualTo("packs/pf2e/feats/some-feat.json")
            assertThat(info.category).isEqualTo("feats")
        }

        @Test
        fun `handles spells category`() {
            val info = OrphanInfo.fromIdAndPath(UUID.randomUUID(), "packs/pf2e/spells/fireball.json")

            assertThat(info.category).isEqualTo("spells")
        }
    }

    private fun createTreeEntry(path: String) = GitHubTreeEntry(
        path = path,
        mode = "100644",
        type = "blob",
        sha = "abc123",
        size = 1000L,
        url = "https://api.github.com/test"
    )

    private fun createTreeResponse(entries: List<GitHubTreeEntry>) = GitHubTreeResponse(
        sha = "tree-sha",
        url = "https://api.github.com/tree",
        tree = entries,
        truncated = false
    )

    private fun createIdAndPath(id: UUID, path: String): IdAndPath = object : IdAndPath {
        override val id: UUID = id
        override val githubPath: String = path
    }
}
