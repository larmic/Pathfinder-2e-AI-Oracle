package de.larmic.pf2e.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

/**
 * Global exception handler for consistent API error responses.
 *
 * Uses RFC 7807 Problem Details format for all error responses.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(GitHubApiException::class)
    fun handleGitHubApiException(ex: GitHubApiException): ProblemDetail {
        logger.warn("GitHub API error: {}", ex.message, ex)

        val status = when (ex.statusCode) {
            401 -> HttpStatus.UNAUTHORIZED
            403 -> HttpStatus.FORBIDDEN
            404 -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_GATEWAY
        }

        return ProblemDetail.forStatus(status).apply {
            type = URI.create("https://problems.pf2e-oracle.dev/github-api-error")
            title = "GitHub API Error"
            detail = ex.message
            setProperty("statusCode", ex.statusCode)
        }
    }

    @ExceptionHandler(ImportException::class)
    fun handleImportException(ex: ImportException): ProblemDetail {
        logger.error("Import error: {}", ex.message, ex)

        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            type = URI.create("https://problems.pf2e-oracle.dev/import-error")
            title = "Import Error"
            detail = ex.message
            ex.path?.let { setProperty("path", it) }
        }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ProblemDetail {
        logger.warn("Invalid argument: {}", ex.message)

        return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            type = URI.create("https://problems.pf2e-oracle.dev/invalid-argument")
            title = "Invalid Argument"
            detail = ex.message
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ProblemDetail {
        logger.error("Unexpected error", ex)

        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR).apply {
            type = URI.create("https://problems.pf2e-oracle.dev/internal-error")
            title = "Internal Server Error"
            detail = "An unexpected error occurred"
        }
    }
}
