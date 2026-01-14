package de.larmic.pf2e.importer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.larmic.pf2e.domain.ItemType
import de.larmic.pf2e.domain.PathfinderItem
import de.larmic.pf2e.domain.PathfinderItemRepository
import de.larmic.pf2e.github.GitHubClient
import de.larmic.pf2e.github.GitHubTreeEntry
import de.larmic.pf2e.github.GitHubTreeResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PathfinderImportServiceTest {

    private lateinit var gitHubClient: GitHubClient
    private lateinit var repository: PathfinderItemRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var jobStore: ImportJobStore
    private lateinit var importService: PathfinderImportService

    // In-memory storage for mocked repository
    private val storage = mutableMapOf<String, PathfinderItem>()

    @BeforeEach
    fun setUp() {
        storage.clear()

        gitHubClient = mockk(relaxed = true)
        repository = mockk()
        objectMapper = jacksonObjectMapper()
        jobStore = ImportJobStore()

        // Mock repository behavior
        every { repository.findByGithubPath(any()) } answers {
            storage[firstArg()]
        }
        every { repository.save(any()) } answers {
            val item = firstArg<PathfinderItem>()
            storage[item.githubPath] = item
            item
        }
        every { repository.count() } answers { storage.size.toLong() }

        importService = PathfinderImportService(gitHubClient, repository, objectMapper, jobStore)

        // Default mock für Branch
        every { gitHubClient.getDefaultBranch() } returns "main"
    }

    @Nested
    inner class ChangeDetection {

        @Test
        fun `imports new files`() {
            val tree = createTreeResponse(
                createTreeEntry("packs/pf2e/feats/test.json", "sha-123")
            )
            every { gitHubClient.getTree(any()) } returns tree
            every { gitHubClient.filterTreeEntries(tree, "packs/pf2e/feats") } returns tree.tree
            every { gitHubClient.getRawContent("packs/pf2e/feats/test.json", any()) } returns
                """{"_id": "foundry-1", "name": "Test Feat"}"""

            val result = importService.importFeats()

            assertThat(result.imported).isEqualTo(1)
            assertThat(result.skipped).isEqualTo(0)
            assertThat(repository.count()).isEqualTo(1)
        }

        @Test
        fun `skips unchanged files based on sha`() {
            // Pre-populate store with existing item
            val existingItem = PathfinderItem(
                foundryId = "foundry-1",
                itemType = ItemType.FEAT,
                itemName = "Existing Feat",
                rawJsonContent = "{}",
                githubSha = "sha-unchanged",
                githubPath = "packs/pf2e/feats/existing.json"
            )
            repository.save(existingItem)

            val tree = createTreeResponse(
                createTreeEntry("packs/pf2e/feats/existing.json", "sha-unchanged")
            )
            every { gitHubClient.getTree(any()) } returns tree
            every { gitHubClient.filterTreeEntries(tree, "packs/pf2e/feats") } returns tree.tree

            val result = importService.importFeats()

            assertThat(result.imported).isEqualTo(0)
            assertThat(result.skipped).isEqualTo(1)
            verify(exactly = 0) { gitHubClient.getRawContent(any(), any()) }
        }

        @Test
        fun `reimports files with changed sha`() {
            // Pre-populate store with existing item
            val existingItem = PathfinderItem(
                foundryId = "foundry-1",
                itemType = ItemType.FEAT,
                itemName = "Old Name",
                rawJsonContent = "{}",
                githubSha = "sha-old",
                githubPath = "packs/pf2e/feats/changed.json"
            )
            repository.save(existingItem)

            val tree = createTreeResponse(
                createTreeEntry("packs/pf2e/feats/changed.json", "sha-new")
            )
            every { gitHubClient.getTree(any()) } returns tree
            every { gitHubClient.filterTreeEntries(tree, "packs/pf2e/feats") } returns tree.tree
            every { gitHubClient.getRawContent("packs/pf2e/feats/changed.json", any()) } returns
                """{"_id": "foundry-1", "name": "Updated Name"}"""

            val result = importService.importFeats()

            assertThat(result.imported).isEqualTo(1)
            assertThat(result.skipped).isEqualTo(0)
        }

        @Test
        fun `handles mixed scenario with new unchanged and changed files`() {
            // Existing unchanged item
            repository.save(PathfinderItem(
                foundryId = "1",
                itemType = ItemType.FEAT,
                itemName = "Unchanged",
                rawJsonContent = "{}",
                githubSha = "sha-same",
                githubPath = "packs/pf2e/feats/unchanged.json"
            ))
            // Existing changed item
            repository.save(PathfinderItem(
                foundryId = "2",
                itemType = ItemType.FEAT,
                itemName = "Changed",
                rawJsonContent = "{}",
                githubSha = "sha-old",
                githubPath = "packs/pf2e/feats/changed.json"
            ))

            val tree = createTreeResponse(
                createTreeEntry("packs/pf2e/feats/unchanged.json", "sha-same"),
                createTreeEntry("packs/pf2e/feats/changed.json", "sha-new"),
                createTreeEntry("packs/pf2e/feats/new.json", "sha-brand-new")
            )
            every { gitHubClient.getTree(any()) } returns tree
            every { gitHubClient.filterTreeEntries(tree, "packs/pf2e/feats") } returns tree.tree
            every { gitHubClient.getRawContent("packs/pf2e/feats/changed.json", any()) } returns
                """{"_id": "2", "name": "Changed Updated"}"""
            every { gitHubClient.getRawContent("packs/pf2e/feats/new.json", any()) } returns
                """{"_id": "3", "name": "Brand New"}"""

            val result = importService.importFeats()

            assertThat(result.imported).isEqualTo(2) // changed + new
            assertThat(result.skipped).isEqualTo(1)  // unchanged
            assertThat(result.totalFiles).isEqualTo(3)
        }
    }

    @Nested
    inner class TruncatedTree {

        @Test
        fun `handles truncated tree gracefully`() {
            val tree = GitHubTreeResponse(
                sha = "root",
                url = "https://api.github.com/test",
                tree = listOf(createTreeEntry("packs/pf2e/feats/test.json", "sha-1")),
                truncated = true
            )
            every { gitHubClient.getTree(any()) } returns tree
            every { gitHubClient.filterTreeEntries(tree, "packs/pf2e/feats") } returns tree.tree
            every { gitHubClient.getRawContent(any(), any()) } returns """{"_id": "1", "name": "Test"}"""

            val result = importService.importFeats()

            assertThat(result.imported).isEqualTo(1)
        }
    }

    private fun createTreeResponse(vararg entries: GitHubTreeEntry) = GitHubTreeResponse(
        sha = "root-sha",
        url = "https://api.github.com/test",
        tree = entries.toList(),
        truncated = false
    )

    private fun createTreeEntry(path: String, sha: String) = GitHubTreeEntry(
        path = path,
        mode = "100644",
        type = "blob",
        sha = sha,
        size = 100,
        url = "https://api.github.com/test"
    )
}
