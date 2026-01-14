package de.larmic.pf2e.exception

/**
 * Exception thrown when GitHub API calls fail.
 */
class GitHubApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    companion object {
        fun unauthorized(message: String = "GitHub API authentication failed") =
            GitHubApiException(message, statusCode = 401)

        fun forbidden(message: String = "GitHub API access forbidden - rate limit may be exceeded") =
            GitHubApiException(message, statusCode = 403)

        fun notFound(resource: String) =
            GitHubApiException("GitHub resource not found: $resource", statusCode = 404)

        fun serverError(message: String = "GitHub API server error") =
            GitHubApiException(message, statusCode = 500)
    }
}
