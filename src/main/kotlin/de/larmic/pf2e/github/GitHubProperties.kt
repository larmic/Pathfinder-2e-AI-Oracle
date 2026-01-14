package de.larmic.pf2e.github

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github")
data class GitHubProperties(
    val token: String? = null,
    val repository: String = "foundryvtt/pf2e",
    val maxParallelDownloads: Int = 10
)
