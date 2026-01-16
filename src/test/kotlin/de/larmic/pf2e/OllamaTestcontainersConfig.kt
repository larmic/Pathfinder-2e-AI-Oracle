package de.larmic.pf2e

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.ollama.OllamaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers configuration for integration tests requiring Ollama embeddings.
 *
 * This configuration starts both PostgreSQL (with pgvector) and Ollama containers.
 * The Ollama container automatically pulls the nomic-embed-text model.
 *
 * Note: Model pull can take 1-2 minutes on first run. Use @Tag("integration")
 * to separate these slow tests from fast unit tests.
 *
 * Usage:
 * ```
 * @SpringBootTest
 * @Import(OllamaTestcontainersConfig::class)
 * @Tag("integration")
 * class MyIntegrationTest { ... }
 * ```
 */
@TestConfiguration(proxyBeanMethods = false)
class OllamaTestcontainersConfig {

    companion object {
        private const val EMBEDDING_MODEL = "nomic-embed-text"
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withDatabaseName("pf2e_oracle")
            .withUsername("pf2e")
            .withPassword("pf2e")
            .withReuse(true)

    @Bean
    fun ollamaContainer(): OllamaContainer =
        OllamaContainer(DockerImageName.parse("ollama/ollama:0.5.4"))
            .withReuse(true)

    @Bean
    fun ollamaPropertyRegistrar(ollamaContainer: OllamaContainer): DynamicPropertyRegistrar {
        return DynamicPropertyRegistrar { registry ->
            // Ensure container is running
            if (!ollamaContainer.isRunning) {
                ollamaContainer.start()
            }

            // Pull the embedding model if not present
            ollamaContainer.execInContainer("ollama", "pull", EMBEDDING_MODEL)

            // Register Ollama properties
            registry.add("spring.ai.ollama.base-url") { ollamaContainer.endpoint }
            registry.add("spring.ai.ollama.embedding.model") { EMBEDDING_MODEL }
        }
    }
}
