package de.larmic.pf2e.ingestion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ingestion")
data class IngestionProperties(
    val maxParallelEmbeddings: Int = 5,
    val batchSize: Int = 50
)
