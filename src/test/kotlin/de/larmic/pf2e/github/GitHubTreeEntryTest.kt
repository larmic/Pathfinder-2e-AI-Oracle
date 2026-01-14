package de.larmic.pf2e.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GitHubTreeEntryTest {

    @Nested
    inner class TypeDetection {

        @Test
        fun `isFile returns true for blob type`() {
            val entry = createEntry(type = "blob")

            assertThat(entry.isFile()).isTrue()
            assertThat(entry.isDirectory()).isFalse()
        }

        @Test
        fun `isDirectory returns true for tree type`() {
            val entry = createEntry(type = "tree")

            assertThat(entry.isDirectory()).isTrue()
            assertThat(entry.isFile()).isFalse()
        }
    }

    @Nested
    inner class JsonFileDetection {

        @Test
        fun `isJsonFile returns true for json blob`() {
            val entry = createEntry(type = "blob", path = "packs/pf2e/feats/test.json")

            assertThat(entry.isJsonFile()).isTrue()
        }

        @Test
        fun `isJsonFile returns false for non-json blob`() {
            val entry = createEntry(type = "blob", path = "packs/pf2e/feats/readme.md")

            assertThat(entry.isJsonFile()).isFalse()
        }

        @Test
        fun `isJsonFile returns false for json directory`() {
            val entry = createEntry(type = "tree", path = "packs/pf2e/feats.json")

            assertThat(entry.isJsonFile()).isFalse()
        }
    }

    @Nested
    inner class FilterTreeEntries {

        @Test
        fun `filters only json files under given prefix`() {
            val entries = listOf(
                createEntry(type = "blob", path = "packs/pf2e/feats/ancestry/test.json"),
                createEntry(type = "blob", path = "packs/pf2e/spells/fireball.json"),
                createEntry(type = "blob", path = "packs/pf2e/feats/readme.md"),
                createEntry(type = "tree", path = "packs/pf2e/feats/ancestry"),
                createEntry(type = "blob", path = "packs/pf2e/feats/class/fighter.json"),
            )

            val filtered = entries.filter { it.isJsonFile() && it.path.startsWith("packs/pf2e/feats") }

            assertThat(filtered).hasSize(2)
            assertThat(filtered.map { it.path }).containsExactlyInAnyOrder(
                "packs/pf2e/feats/ancestry/test.json",
                "packs/pf2e/feats/class/fighter.json"
            )
        }

        @Test
        fun `returns empty list when no matches`() {
            val entries = listOf(
                createEntry(type = "blob", path = "packs/pf2e/spells/fireball.json"),
                createEntry(type = "tree", path = "packs/pf2e/feats"),
            )

            val filtered = entries.filter { it.isJsonFile() && it.path.startsWith("packs/pf2e/feats/") }

            assertThat(filtered).isEmpty()
        }
    }

    private fun createEntry(
        type: String = "blob",
        path: String = "test.json",
        sha: String = "abc123"
    ) = GitHubTreeEntry(
        path = path,
        mode = "100644",
        type = type,
        sha = sha,
        size = 100,
        url = "https://api.github.com/test"
    )
}
