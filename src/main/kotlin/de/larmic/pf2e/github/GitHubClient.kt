package de.larmic.pf2e.github

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@EnableConfigurationProperties(GitHubProperties::class)
class GitHubClient(
    private val properties: GitHubProperties
) {

    private val log = LoggerFactory.getLogger(GitHubClient::class.java)

    private val apiClient = RestClient.builder()
        .baseUrl("https://api.github.com")
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
        .defaultHeader(HttpHeaders.USER_AGENT, "pf2e-oracle/1.0 (Pathfinder 2e AI Oracle)")
        .defaultHeaders { headers ->
            properties.token?.takeIf { it.isNotBlank() }?.let { token ->
                headers.setBearerAuth(token)
                log.info("GitHub API Client initialisiert mit Token-Authentifizierung")
            } ?: log.warn("GitHub API Client ohne Token - Rate Limit: 60 Requests/Stunde")
        }
        .build()

    private val rawClient = RestClient.builder()
        .baseUrl("https://raw.githubusercontent.com")
        .defaultHeader(HttpHeaders.USER_AGENT, "pf2e-oracle/1.0 (Pathfinder 2e AI Oracle)")
        .build()

    private var cachedDefaultBranch: String? = null

    fun getDefaultBranch(): String {
        cachedDefaultBranch?.let { return it }

        log.info("Ermittle Default Branch für {}", properties.repository)
        val response = apiClient.get()
            .uri("/repos/${properties.repository}")
            .retrieve()
            .body(GitHubRepoResponse::class.java)

        val branch = response?.defaultBranch ?: "main"
        cachedDefaultBranch = branch
        log.info("Default Branch: {}", branch)
        return branch
    }

    fun getTree(branch: String = getDefaultBranch()): GitHubTreeResponse {
        log.info("Lade Tree für Branch: {}", branch)

        return apiClient.get()
            .uri("/repos/${properties.repository}/git/trees/$branch?recursive=1")
            .retrieve()
            .body(GitHubTreeResponse::class.java)
            ?: throw RuntimeException("Konnte Tree nicht laden")
    }

    fun getRawContent(path: String, branch: String = getDefaultBranch()): String {
        log.debug("Lade Raw Content: {}", path)

        return rawClient.get()
            .uri("/${properties.repository}/$branch/$path")
            .retrieve()
            .body(String::class.java)
            ?: throw RuntimeException("Konnte Datei nicht laden: $path")
    }

    fun filterTreeEntries(tree: GitHubTreeResponse, pathPrefix: String): List<GitHubTreeEntry> {
        return tree.tree.filter { entry ->
            entry.isJsonFile()
                && entry.path.startsWith(pathPrefix)
                && !entry.path.endsWith("_folders.json")
        }
    }
}
