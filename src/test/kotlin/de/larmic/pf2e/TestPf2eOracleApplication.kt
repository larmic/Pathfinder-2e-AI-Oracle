package de.larmic.pf2e

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers configuration for local development and testing.
 * Allows debugging the application in IDE without manually starting PostgreSQL.
 *
 * Usage: Run the main() function below in IntelliJ to start the app with auto-started DB.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withDatabaseName("pf2e_oracle")
            .withUsername("pf2e")
            .withPassword("pf2e")
            .withReuse(true)
}

fun main(args: Array<String>) {
    fromApplication<Pf2eOracleApplication>()
        .with(TestcontainersConfig::class)
        .run(*args)
}
