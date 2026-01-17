package de.larmic.pf2e.cleanup

import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.github.GitHubClient
import de.larmic.pf2e.importer.FoundryImportService.Companion.PATH_PREFIX
import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * Service for detecting and cleaning up orphaned entries.
 *
 * Orphans are database entries whose corresponding files no longer exist
 * in the GitHub repository. This service compares the current GitHub tree
 * with stored paths and removes entries that are no longer present.
 */
@Service
class OrphanCleanupService(
    private val gitHubClient: GitHubClient,
    private val repository: FoundryRawEntryRepository,
    private val vectorStore: VectorStore
) {

    private val log = LoggerFactory.getLogger(OrphanCleanupService::class.java)

    /**
     * Detects orphans without deleting them (dry-run/preview).
     *
     * @return List of orphan information with path and category
     */
    fun detectOrphans(): List<OrphanInfo> {
        log.info("Detecting orphans...")

        val githubPaths = fetchGitHubPaths()
        val storedEntries = repository.findAllIdAndPaths()

        val orphans = storedEntries
            .filter { it.githubPath !in githubPaths }
            .map { OrphanInfo.fromIdAndPath(it.id, it.githubPath) }

        log.info("Detected {} orphans out of {} stored entries", orphans.size, storedEntries.size)
        return orphans
    }

    /**
     * Cleans up orphaned entries from both database and vector store.
     *
     * @return Result containing counts and paths of deleted entries
     */
    @Transactional
    fun cleanupOrphans(): CleanupResult {
        val startTime = Instant.now()
        log.info("Starting orphan cleanup...")

        val orphans = detectOrphans()

        if (orphans.isEmpty()) {
            log.info("No orphans found, nothing to clean up")
            return CleanupResult(
                deletedFromDatabase = 0,
                deletedFromVectorStore = 0,
                orphanPaths = emptyList(),
                durationMs = Duration.between(startTime, Instant.now()).toMillis()
            )
        }

        val orphanIds = orphans.map { it.id }
        val orphanPaths = orphans.map { it.githubPath }

        // Delete from vector store first (uses document ID = entry.id)
        val vectorDeleteCount = deleteFromVectorStore(orphanIds)

        // Then delete from database
        repository.deleteAllByIds(orphanIds)
        log.info("Deleted {} entries from database", orphanIds.size)

        val duration = Duration.between(startTime, Instant.now())
        log.info("Orphan cleanup completed: {} DB entries, {} vector store entries in {}ms",
            orphanIds.size, vectorDeleteCount, duration.toMillis())

        return CleanupResult(
            deletedFromDatabase = orphanIds.size,
            deletedFromVectorStore = vectorDeleteCount,
            orphanPaths = orphanPaths,
            durationMs = duration.toMillis()
        )
    }

    private fun fetchGitHubPaths(): Set<String> {
        val tree = gitHubClient.getTree()
        if (tree.truncated) {
            log.warn("GitHub tree was truncated - orphan detection may be incomplete")
        }

        return gitHubClient.filterTreeEntries(tree, PATH_PREFIX)
            .map { it.path }
            .toSet()
    }

    private fun deleteFromVectorStore(ids: List<java.util.UUID>): Int {
        if (ids.isEmpty()) return 0

        return try {
            val stringIds = ids.map { it.toString() }
            vectorStore.delete(stringIds)
            log.info("Deleted {} entries from vector store", ids.size)
            ids.size
        } catch (e: Exception) {
            log.error("Failed to delete from vector store: {}", e.message, e)
            0
        }
    }
}
