package de.larmic.pf2e.importer

import com.ninjasquad.springmockk.MockkBean
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(ImportController::class)
@Import(ImportJobStore::class)
class ImportControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var importService: FoundryImportService

    @MockkBean
    private lateinit var repository: FoundryRawEntryRepository

    @Autowired
    private lateinit var jobStore: ImportJobStore

    @BeforeEach
    fun setUp() {
        jobStore.clear()
    }

    @Nested
    inner class PostImportAll {

        @Test
        fun `returns 202 Accepted`() {
            mockMvc.perform(post("/api/import/all"))
                .andExpect(status().isAccepted)
        }

        @Test
        fun `returns Location header with job URL`() {
            mockMvc.perform(post("/api/import/all"))
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/import/jobs/")))
        }

        @Test
        fun `returns job in response body`() {
            mockMvc.perform(post("/api/import/all"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.itemType").value("ALL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
        }
    }

    @Nested
    inner class PostImportCategory {

        @Test
        fun `returns 202 Accepted`() {
            mockMvc.perform(post("/api/import/feats"))
                .andExpect(status().isAccepted)
        }

        @Test
        fun `returns Location header with job URL`() {
            mockMvc.perform(post("/api/import/spells"))
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/import/jobs/")))
        }

        @Test
        fun `uses uppercase category as itemType`() {
            mockMvc.perform(post("/api/import/actions"))
                .andExpect(jsonPath("$.itemType").value("ACTIONS"))
        }
    }

    @Nested
    inner class GetCategories {

        @Test
        fun `returns list of categories`() {
            every { importService.getAvailableCategories() } returns listOf("actions", "feats", "spells")

            mockMvc.perform(get("/api/import/categories"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("actions"))
        }

        @Test
        fun `returns empty list when no categories`() {
            every { importService.getAvailableCategories() } returns emptyList()

            mockMvc.perform(get("/api/import/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class GetJobStatus {

        @Test
        fun `returns job when exists`() {
            val job = jobStore.create("FEATS")

            mockMvc.perform(get("/api/import/jobs/${job.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(job.id.toString()))
                .andExpect(jsonPath("$.itemType").value("FEATS"))
        }

        @Test
        fun `returns 404 when job not found`() {
            val randomId = UUID.randomUUID()

            mockMvc.perform(get("/api/import/jobs/$randomId"))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class GetAllJobs {

        @Test
        fun `returns all jobs`() {
            jobStore.create("FEATS")
            jobStore.create("SPELLS")

            mockMvc.perform(get("/api/import/jobs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
        }

        @Test
        fun `returns empty list when no jobs`() {
            mockMvc.perform(get("/api/import/jobs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class GetStats {

        @Test
        fun `returns total count and byType map`() {
            every { repository.count() } returns 100L
            every { repository.findAllFoundryTypes() } returns listOf("feat", "spell")
            every { repository.countByFoundryType("feat") } returns 60L
            every { repository.countByFoundryType("spell") } returns 40L

            mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.byType.feat").value(60))
                .andExpect(jsonPath("$.byType.spell").value(40))
        }

        @Test
        fun `returns empty stats when no data`() {
            every { repository.count() } returns 0L
            every { repository.findAllFoundryTypes() } returns emptyList()

            mockMvc.perform(get("/api/import/stats"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.byType").isEmpty)
        }
    }
}
