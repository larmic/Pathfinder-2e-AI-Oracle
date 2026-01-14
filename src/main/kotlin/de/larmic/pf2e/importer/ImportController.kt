package de.larmic.pf2e.importer

import de.larmic.pf2e.domain.FoundryRawEntryRepository
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
    private val importService: FoundryImportService,
    private val repository: FoundryRawEntryRepository,
    private val jobStore: ImportJobStore
) {

    private val scope = CoroutineScope(Dispatchers.IO)

    @PostMapping("/all")
    fun importAll(): ResponseEntity<ImportJob> {
        val job = jobStore.create("ALL")

        scope.launch {
            try {
                val result = importService.importAll(job.id)
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

    @PostMapping("/{category}")
    fun importCategory(@PathVariable category: String): ResponseEntity<ImportJob> {
        val job = jobStore.create(category.uppercase())

        scope.launch {
            try {
                val result = importService.importCategory(category, job.id)
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

    @GetMapping("/categories")
    fun getCategories(): List<String> = importService.getAvailableCategories()

    @GetMapping("/jobs/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<ImportJob> {
        val job = jobStore.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/jobs")
    fun getAllJobs(): List<ImportJob> = jobStore.findAll()

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        val types = repository.findAllFoundryTypes()
        return mapOf(
            "total" to repository.count(),
            "byType" to types.associateWith { repository.countByFoundryType(it) }
        )
    }
}
