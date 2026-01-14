package de.larmic.pf2e.importer

import com.fasterxml.jackson.databind.ObjectMapper
import de.larmic.pf2e.domain.ItemType
import de.larmic.pf2e.domain.PathfinderItem
import de.larmic.pf2e.domain.PathfinderItemStore
import de.larmic.pf2e.github.GitHubClient
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
class PathfinderImportService(
    private val gitHubClient: GitHubClient,
    private val itemStore: PathfinderItemStore,
    private val objectMapper: ObjectMapper,
    private val jobStore: ImportJobStore
) {

    private val log = LoggerFactory.getLogger(PathfinderImportService::class.java)

    companion object {
        const val MAX_PARALLEL_DOWNLOADS = 10
        const val PATH_PREFIX = "packs/pf2e"
    }

    fun importFeats(jobId: UUID? = null): ImportResult = importItems("$PATH_PREFIX/feats", ItemType.FEAT, jobId)

    fun importSpells(jobId: UUID? = null): ImportResult = importItems("$PATH_PREFIX/spells", ItemType.SPELL, jobId)

    fun importItems(pathPrefix: String, itemType: ItemType, jobId: UUID? = null): ImportResult {
        val startTime = Instant.now()
        log.info("Starte Import von {} (Pfad: {})", itemType, pathPrefix)

        // 1. Tree laden (1 API-Call)
        val tree = gitHubClient.getTree()
        if (tree.truncated) {
            log.warn("Tree wurde abgeschnitten - einige Dateien fehlen möglicherweise")
        }

        // 2. Relevante Einträge filtern
        val entries = gitHubClient.filterTreeEntries(tree, pathPrefix)
        log.info("Gefunden: {} JSON-Dateien unter {}", entries.size, pathPrefix)

        // 3. Change-Detection: Nur geänderte/neue Dateien
        val toImport = entries.filter { entry ->
            val existing = itemStore.findByGithubPath(entry.path)
            existing == null || existing.githubSha != entry.sha
        }
        val skipped = entries.size - toImport.size
        log.info("Zu importieren: {} (übersprungen: {} unverändert)", toImport.size, skipped)

        // Job-Status aktualisieren
        jobId?.let { jobStore.start(it, toImport.size) }

        // 4. Parallel Download mit Throttling
        val imported = AtomicInteger(0)
        val errors = AtomicInteger(0)
        val processed = AtomicInteger(0)
        val total = toImport.size
        val semaphore = Semaphore(MAX_PARALLEL_DOWNLOADS)
        val executor = Executors.newVirtualThreadPerTaskExecutor()

        val futures = toImport.map { entry ->
            executor.submit {
                semaphore.acquire()
                try {
                    processEntry(entry, itemType)
                    imported.incrementAndGet()
                } catch (e: Exception) {
                    log.error("Fehler bei {}: {}", entry.path, e.message)
                    errors.incrementAndGet()
                } finally {
                    semaphore.release()
                    val current = processed.incrementAndGet()

                    // Progress-Update alle 50 Dateien oder am Ende
                    if (current % 50 == 0 || current == total) {
                        jobId?.let { jobStore.updateProgress(it, current, skipped) }
                        val percent = (current * 100) / total
                        log.info("Fortschritt: {}/{} ({}%)", current, total, percent)
                    }
                }
            }
        }

        // Warten bis alle fertig
        futures.forEach { it.get() }
        executor.shutdown()

        val duration = Duration.between(startTime, Instant.now())
        log.info("Import abgeschlossen: {} importiert, {} übersprungen, {} Fehler in {}s",
            imported.get(), skipped, errors.get(), duration.seconds)

        return ImportResult(
            imported = imported.get(),
            skipped = skipped,
            errors = errors.get(),
            durationSeconds = duration.seconds,
            totalFiles = entries.size
        )
    }

    private fun processEntry(entry: GitHubTreeEntry, itemType: ItemType) {
        val rawJson = gitHubClient.getRawContent(entry.path)
        val jsonNode = objectMapper.readTree(rawJson)

        val item = PathfinderItem(
            foundryId = jsonNode.get("_id")?.asText() ?: "",
            itemType = itemType,
            itemName = jsonNode.get("name")?.asText() ?: entry.path.substringAfterLast("/"),
            rawJsonContent = rawJson,
            githubSha = entry.sha,
            githubPath = entry.path
        )

        itemStore.save(item)
        log.debug("Importiert: {} ({})", item.itemName, item.githubPath)
    }
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: Int,
    val durationSeconds: Long,
    val totalFiles: Int
)
