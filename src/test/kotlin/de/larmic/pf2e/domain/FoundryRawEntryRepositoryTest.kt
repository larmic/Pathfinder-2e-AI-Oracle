package de.larmic.pf2e.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class FoundryRawEntryRepositoryTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withDatabaseName("pf2e_oracle")
            .withUsername("pf2e")
            .withPassword("pf2e")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.url") { postgres.jdbcUrl }
            registry.add("spring.flyway.user") { postgres.username }
            registry.add("spring.flyway.password") { postgres.password }
        }
    }

    @Autowired
    lateinit var repository: FoundryRawEntryRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Nested
    inner class Save {

        @Test
        fun `saves and retrieves entry`() {
            val entry = createEntry(name = "Test Feat")

            val saved = repository.save(entry)

            assertThat(saved.id).isEqualTo(entry.id)
            assertThat(repository.count()).isEqualTo(1)
        }

        @Test
        fun `updates entry with same id`() {
            val entry = createEntry(name = "Original")
            repository.save(entry)

            val updated = entry.copy(name = "Updated")
            repository.save(updated)

            assertThat(repository.count()).isEqualTo(1)
            assertThat(repository.findById(entry.id).get().name).isEqualTo("Updated")
        }
    }

    @Nested
    inner class FindByGithubPath {

        @Test
        fun `finds entry by github path`() {
            val entry = createEntry(githubPath = "packs/pf2e/feats/test.json")
            repository.save(entry)

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
        fun `finds entry by foundry id`() {
            val entry = createEntry(foundryId = "abc123")
            repository.save(entry)

            val found = repository.findByFoundryId("abc123")

            assertThat(found).isNotNull
            assertThat(found?.foundryId).isEqualTo("abc123")
        }
    }

    @Nested
    inner class FindAllByFoundryType {

        @Test
        fun `filters entries by type`() {
            repository.save(createEntry(foundryType = "feat", name = "Feat 1", githubPath = "feat1.json"))
            repository.save(createEntry(foundryType = "feat", name = "Feat 2", githubPath = "feat2.json"))
            repository.save(createEntry(foundryType = "spell", name = "Spell 1", githubPath = "spell1.json"))

            val feats = repository.findAllByFoundryType("feat")
            val spells = repository.findAllByFoundryType("spell")

            assertThat(feats).hasSize(2)
            assertThat(spells).hasSize(1)
        }

        @Test
        fun `returns empty list when no entries of type`() {
            repository.save(createEntry(foundryType = "feat"))

            val spells = repository.findAllByFoundryType("spell")

            assertThat(spells).isEmpty()
        }
    }

    @Nested
    inner class CountByFoundryType {

        @Test
        fun `counts entries by type`() {
            repository.save(createEntry(foundryType = "feat", foundryId = "1", githubPath = "f1.json"))
            repository.save(createEntry(foundryType = "feat", foundryId = "2", githubPath = "f2.json"))
            repository.save(createEntry(foundryType = "spell", foundryId = "3", githubPath = "s1.json"))

            assertThat(repository.countByFoundryType("feat")).isEqualTo(2)
            assertThat(repository.countByFoundryType("spell")).isEqualTo(1)
            assertThat(repository.countByFoundryType("action")).isEqualTo(0)
        }
    }

    @Nested
    inner class FindAllFoundryTypes {

        @Test
        fun `returns all distinct types sorted`() {
            repository.save(createEntry(foundryType = "spell", foundryId = "1", githubPath = "s1.json"))
            repository.save(createEntry(foundryType = "feat", foundryId = "2", githubPath = "f1.json"))
            repository.save(createEntry(foundryType = "action", foundryId = "3", githubPath = "a1.json"))
            repository.save(createEntry(foundryType = "feat", foundryId = "4", githubPath = "f2.json"))

            val types = repository.findAllFoundryTypes()

            assertThat(types).containsExactly("action", "feat", "spell")
        }

        @Test
        fun `returns empty list when no entries`() {
            val types = repository.findAllFoundryTypes()

            assertThat(types).isEmpty()
        }
    }

    private fun createEntry(
        foundryId: String = "foundry-123",
        foundryType: String = "feat",
        name: String = "Test Entry",
        githubPath: String = "packs/pf2e/feats/test.json",
        githubSha: String = "sha-abc"
    ) = FoundryRawEntry(
        foundryId = foundryId,
        foundryType = foundryType,
        name = name,
        rawJsonContent = """{"name": "$name"}""",
        githubSha = githubSha,
        githubPath = githubPath
    )
}
