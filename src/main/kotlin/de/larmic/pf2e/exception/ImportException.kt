package de.larmic.pf2e.exception

/**
 * Exception thrown when import operations fail.
 */
class ImportException(
    message: String,
    val path: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    companion object {
        fun jsonParseFailed(path: String, cause: Throwable? = null) =
            ImportException("Failed to parse JSON: $path", path = path, cause = cause)

        fun missingField(path: String, fieldName: String) =
            ImportException("Missing required field '$fieldName' in: $path", path = path)

        fun downloadFailed(path: String, cause: Throwable? = null) =
            ImportException("Failed to download: $path", path = path, cause = cause)
    }
}
