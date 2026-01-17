package de.larmic.pf2e.cleanup

import java.util.UUID

data class CleanupResult(
    val deletedFromDatabase: Int,
    val deletedFromVectorStore: Int,
    val orphanPaths: List<String>,
    val durationMs: Long
)

data class OrphanInfo(
    val id: UUID,
    val githubPath: String,
    val category: String
) {
    companion object {
        fun fromIdAndPath(id: UUID, path: String): OrphanInfo {
            val category = path
                .removePrefix("packs/pf2e/")
                .substringBefore("/")
            return OrphanInfo(id, path, category)
        }
    }
}
