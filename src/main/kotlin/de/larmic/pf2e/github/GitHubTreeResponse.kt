package de.larmic.pf2e.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeEntry>,
    val truncated: Boolean
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubTreeEntry(
    val path: String,
    val mode: String,
    val type: String, // "blob" (file) oder "tree" (directory)
    val sha: String,
    val size: Long?,
    val url: String?
) {
    fun isFile(): Boolean = type == "blob"
    fun isDirectory(): Boolean = type == "tree"
    fun isJsonFile(): Boolean = isFile() && path.endsWith(".json")
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubRepoResponse(
    @param:JsonProperty("default_branch")
    val defaultBranch: String
)
