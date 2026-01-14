package de.larmic.pf2e.importer

import de.larmic.pf2e.domain.ItemType
import de.larmic.pf2e.domain.PathfinderItemStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/api/import")
class ImportController(
    private val importService: PathfinderImportService,
    private val itemStore: PathfinderItemStore,
    private val jobStore: ImportJobStore
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    @PostMapping("/feats")
    fun importFeats(): ResponseEntity<ImportJob> {
        val job = jobStore.create("FEAT")

        scope.launch {
            try {
                val result = importService.importFeats(job.id)
                jobStore.complete(job.id, result)
            } catch (e: Exception) {
                jobStore.fail(job.id, e.message ?: "Unknown error")
            }
        }

        return ResponseEntity
            .accepted()
            .location(URI.create("/api/import/jobs/${job.id}"))
            .body(job)
    }

    @PostMapping("/spells")
    fun importSpells(): ResponseEntity<ImportJob> {
        val job = jobStore.create("SPELL")

        scope.launch {
            try {
                val result = importService.importSpells(job.id)
                jobStore.complete(job.id, result)
            } catch (e: Exception) {
                jobStore.fail(job.id, e.message ?: "Unknown error")
            }
        }

        return ResponseEntity
            .accepted()
            .location(URI.create("/api/import/jobs/${job.id}"))
            .body(job)
    }

    @GetMapping("/jobs/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<ImportJob> {
        val job = jobStore.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/jobs")
    fun getAllJobs(): List<ImportJob> = jobStore.findAll()

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> = mapOf(
        "total" to itemStore.count(),
        "byType" to ItemType.entries.associate { it.name to itemStore.countByType(it) }
    )
}
