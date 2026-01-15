package de.larmic.pf2e.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for GitHubClient.
 *
 * Note: The HTTP-calling methods (getDefaultBranch, getTree, getRawContent)
 * are difficult to unit test because RestClient is created internally.
 * These methods are tested indirectly through FoundryImportServiceTest
 * where GitHubClient is mocked.
 *
 * Future improvement: Inject RestClient.Builder to enable proper mocking.
 */
class GitHubClientTest {

    private fun createTreeEntry(path: String, type: String, sha: String) = GitHubTreeEntry(
        path = path,
        mode = "100644",
        type = type,
        sha = sha,
        size = if (type == "blob") 100L else null,
        url = "https://api.github.com/repos/test/$sha"
    )

    private fun createTreeResponse(entries: List<GitHubTreeEntry>, truncated: Boolean = false) = GitHubTreeResponse(
        sha = "tree-sha",
        url = "https://api.github.com/repos/test/tree",
        tree = entries,
        truncated = truncated
    )

    @Nested
    inner class FilterTreeEntries {

        @Test
        fun `filters entries by path prefix`() {
            val properties = GitHubProperties()
            val client = GitHubClient(properties)
            val tree = createTreeResponse(listOf(
                createTreeEntry("packs/pf2e/feats/test.json", "blob", "sha1"),
                createTreeEntry("packs/pf2e/spells/fireball.json", "blob", "sha2"),
                createTreeEntry("other/file.json", "blob", "sha3"),
                createTreeEntry("packs/pf2e/feats", "tree", "sha4")
            ))

            val result = client.filterTreeEntries(tree, "packs/pf2e/feats")

            assertThat(result).hasSize(1)
            assertThat(result[0].path).isEqualTo("packs/pf2e/feats/test.json")
        }

        @Test
        fun `returns empty list when no matches`() {
            val properties = GitHubProperties()
            val client = GitHubClient(properties)
            val tree = createTreeResponse(listOf(
                createTreeEntry("other/file.json", "blob", "sha1")
            ))

            val result = client.filterTreeEntries(tree, "packs/pf2e")

            assertThat(result).isEmpty()
        }

        @Test
        fun `excludes non-JSON files`() {
            val properties = GitHubProperties()
            val client = GitHubClient(properties)
            val tree = createTreeResponse(listOf(
                createTreeEntry("packs/pf2e/feats/test.json", "blob", "sha1"),
                createTreeEntry("packs/pf2e/feats/readme.md", "blob", "sha2"),
                createTreeEntry("packs/pf2e/feats/image.png", "blob", "sha3")
            ))

            val result = client.filterTreeEntries(tree, "packs/pf2e/feats")

            assertThat(result).hasSize(1)
            assertThat(result[0].path).endsWith(".json")
        }

        @Test
        fun `excludes directories`() {
            val properties = GitHubProperties()
            val client = GitHubClient(properties)
            val tree = createTreeResponse(listOf(
                createTreeEntry("packs/pf2e/feats/test.json", "blob", "sha1"),
                createTreeEntry("packs/pf2e/feats/subfolder", "tree", "sha2")
            ))

            val result = client.filterTreeEntries(tree, "packs/pf2e/feats")

            assertThat(result).hasSize(1)
            assertThat(result.all { it.isFile() }).isTrue()
        }

        @Test
        fun `excludes _folders json files`() {
            val properties = GitHubProperties()
            val client = GitHubClient(properties)
            val tree = createTreeResponse(listOf(
                createTreeEntry("packs/pf2e/feats/test.json", "blob", "sha1"),
                createTreeEntry("packs/pf2e/feats/_folders.json", "blob", "sha2"),
                createTreeEntry("packs/pf2e/spells/_folders.json", "blob", "sha3"),
                createTreeEntry("packs/pf2e/spells/fireball.json", "blob", "sha4")
            ))

            val result = client.filterTreeEntries(tree, "packs/pf2e")

            assertThat(result).hasSize(2)
            assertThat(result.map { it.path }).containsExactlyInAnyOrder(
                "packs/pf2e/feats/test.json",
                "packs/pf2e/spells/fireball.json"
            )
            assertThat(result.none { it.path.endsWith("_folders.json") }).isTrue()
        }
    }

    @Nested
    inner class GitHubPropertiesDefaults {

        @Test
        fun `has sensible default values`() {
            val properties = GitHubProperties()

            assertThat(properties.token).isNull()
            assertThat(properties.repository).isEqualTo("foundryvtt/pf2e")
            assertThat(properties.maxParallelDownloads).isEqualTo(10)
        }

        @Test
        fun `accepts custom values`() {
            val properties = GitHubProperties(
                token = "ghp_test123",
                repository = "custom/repo",
                maxParallelDownloads = 5
            )

            assertThat(properties.token).isEqualTo("ghp_test123")
            assertThat(properties.repository).isEqualTo("custom/repo")
            assertThat(properties.maxParallelDownloads).isEqualTo(5)
        }
    }
}
