package de.larmic.pf2e.importer

import com.fasterxml.jackson.databind.ObjectMapper
import de.larmic.pf2e.domain.FoundryRawEntry
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import de.larmic.pf2e.github.GitHubClient
import de.larmic.pf2e.github.GitHubProperties
import de.larmic.pf2e.github.GitHubTreeEntry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

@Service
class FoundryImportService(
    private val gitHubClient: GitHubClient,
    private val gitHubProperties: GitHubProperties,
    private val repository: FoundryRawEntryRepository,
    private val objectMapper: ObjectMapper,
    private val jobStore: ImportJobStore
) {

    private val log = LoggerFactory.getLogger(FoundryImportService::class.java)

    companion object {
        const val PATH_PREFIX = "packs/pf2e"
    }

    private fun extractCategory(path: String): String {
        // path: "packs/pf2e/feats/something.json" -> "feats"
        return path.removePrefix("$PATH_PREFIX/").substringBefore("/")
    }

    private fun formatEta(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            seconds > 0 -> "${seconds}s"
            else -> "< 1s"
        }
    }

    /**
     * Returns all available categories under packs/pf2e
     */
    fun getAvailableCategories(): List<String> {
        val tree = gitHubClient.getTree()
        return tree.tree
            .filter { it.path.startsWith(PATH_PREFIX) && it.isDirectory() }
            .map { it.path.removePrefix("$PATH_PREFIX/").substringBefore("/") }
            .distinct()
            .sorted()
    }

    /**
     * Imports all categories from packs/pf2e
     */
    fun importAll(jobId: UUID? = null): ImportResult {
        return importFromPath(PATH_PREFIX, jobId)
    }

    /**
     * Imports a specific category (e.g., "feats", "spells", "actions")
     */
    fun importCategory(category: String, jobId: UUID? = null): ImportResult {
        return importFromPath("$PATH_PREFIX/$category", jobId)
    }

    private fun importFromPath(pathPrefix: String, jobId: UUID?): ImportResult {
        val startTime = Instant.now()
        log.info("Starting import from path: {}", pathPrefix)

        // 1. Load tree (1 API call)
        val tree = gitHubClient.getTree()
        if (tree.truncated) {
            log.warn("Tree was truncated - some files may be missing")
        }

        // 2. Filter relevant entries
        val entries = gitHubClient.filterTreeEntries(tree, pathPrefix)
        log.info("Found: {} JSON files under {}", entries.size, pathPrefix)

        // 3. Change detection: only changed/new files
        val toImport = entries.filter { entry ->
            val existing = repository.findByGithubPath(entry.path)
            existing == null || existing.githubSha != entry.sha
        }
        val skipped = entries.size - toImport.size
        log.info("To import: {} (skipped: {} unchanged)", toImport.size, skipped)

        // Update job status
        jobId?.let { jobStore.start(it, toImport.size) }

        // 4. Parallel download with throttling
        val imported = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val processed = AtomicInteger(0)
        val total = toImport.size
        val semaphore = Semaphore(gitHubProperties.maxParallelDownloads)
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        val futures = toImport.map { entry ->
            executor.submit {
                semaphore.acquire()
                try {
                    processEntry(entry)
                    imported.incrementAndGet()
                } catch (e: Exception) {
                    log.error("Error processing {}: {}", entry.path, e.message)
                    errors.incrementAndGet()
                } finally {
                    semaphore.release()
                    val current = processed.incrementAndGet()

                    // Progress update every 50 files or at the end
                    if (current % 50 == 0 || current == total) {
                        jobId?.let { jobStore.updateProgress(it, current, skipped) }
                        val percent = if (total > 0) (current * 100) / total else 100
                        val category = extractCategory(entry.path)

                        if (current == total) {
                            log.info("Progress: {}/{} ({}%) - done", current, total, percent)
                        } else {
                            val elapsed = Duration.between(startTime, Instant.now())
                            val avgPerFile = elapsed.toMillis() / current.toDouble()
                            val remainingFiles = total - current
                            val etaMillis = (avgPerFile * remainingFiles).toLong()
                            val eta = formatEta(Duration.ofMillis(etaMillis))
                            log.info("Progress: {}/{} ({}%) - current: {} - ETA: {}", current, total, percent, category, eta)
                        }
                    }
                }
            }
        }

        // Wait for all to complete
        futures.forEach { it.get() }
        executor.shutdown()

        val duration = Duration.between(startTime, Instant.now())
        log.info("Import completed: {} imported, {} skipped, {} errors in {}s",
            imported.get(), skipped, errors.get(), duration.seconds)

        return ImportResult(
            imported = imported.get(),
            skipped = skipped,
            errors = errors.get(),
            durationSeconds = duration.seconds,
            totalFiles = entries.size
        )
    }

    private fun processEntry(entry: GitHubTreeEntry) {
        val rawJson = gitHubClient.getRawContent(entry.path)
        val jsonNode = objectMapper.readTree(rawJson)

        val foundryEntry = FoundryRawEntry(
            foundryId = jsonNode.get("_id")?.asText() ?: "",
            foundryType = jsonNode.get("type")?.asText() ?: "unknown",
            name = jsonNode.get("name")?.asText() ?: entry.path.substringAfterLast("/").removeSuffix(".json"),
            rawJsonContent = rawJson,
            githubSha = entry.sha,
            githubPath = entry.path
        )

        repository.save(foundryEntry)
        log.debug("Imported: {} ({}) - type: {}", foundryEntry.name, foundryEntry.githubPath, foundryEntry.foundryType)
    }
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: Int,
    val durationSeconds: Long,
    val totalFiles: Int
)
