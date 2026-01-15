package de.larmic.pf2e.ingestion

import com.ninjasquad.springmockk.MockkBean
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import io.mockk.every
import org.hamcrest.Matchers.startsWith
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

@WebMvcTest(IngestionController::class)
@Import(IngestionJobStore::class)
class IngestionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var ingestionService: IngestionService

    @MockkBean
    private lateinit var repository: FoundryRawEntryRepository

    @Autowired
    private lateinit var jobStore: IngestionJobStore

    @BeforeEach
    fun setUp() {
        jobStore.clear()
    }

    @Nested
    inner class PostIngestAll {

        @Test
        fun `returns 202 Accepted`() {
            mockMvc.perform(post("/api/ingestion/all"))
                .andExpect(status().isAccepted)
        }

        @Test
        fun `returns Location header with job URL`() {
            mockMvc.perform(post("/api/ingestion/all"))
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", startsWith("/api/ingestion/jobs/")))
        }

        @Test
        fun `returns job in response body`() {
            mockMvc.perform(post("/api/ingestion/all"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.foundryType").value("ALL"))
                .andExpect(jsonPath("$.status").value("PENDING"))
        }

        @Test
        fun `accepts force parameter`() {
            mockMvc.perform(post("/api/ingestion/all?force=true"))
                .andExpect(status().isAccepted)
                .andExpect(jsonPath("$.foundryType").value("ALL"))
        }
    }

    @Nested
    inner class GetJobStatus {

        @Test
        fun `returns job when exists`() {
            val job = jobStore.create("ALL")

            mockMvc.perform(get("/api/ingestion/jobs/${job.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(job.id.toString()))
                .andExpect(jsonPath("$.foundryType").value("ALL"))
        }

        @Test
        fun `returns 404 when job not found`() {
            val randomId = UUID.randomUUID()

            mockMvc.perform(get("/api/ingestion/jobs/$randomId"))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class GetAllJobs {

        @Test
        fun `returns all jobs`() {
            jobStore.create("ALL")
            jobStore.create("ALL")

            mockMvc.perform(get("/api/ingestion/jobs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
        }

        @Test
        fun `returns empty list when no jobs`() {
            mockMvc.perform(get("/api/ingestion/jobs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class GetAvailableTypes {

        @Test
        fun `returns list of types with counts`() {
            every { repository.findAllFoundryTypes() } returns listOf("feat", "spell", "action")
            every { repository.countByFoundryType("feat") } returns 100L
            every { repository.countByFoundryType("spell") } returns 200L
            every { repository.countByFoundryType("action") } returns 50L

            mockMvc.perform(get("/api/ingestion/types"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("feat"))
                .andExpect(jsonPath("$[0].count").value(100))
                .andExpect(jsonPath("$[1].type").value("spell"))
                .andExpect(jsonPath("$[1].count").value(200))
                .andExpect(jsonPath("$[2].type").value("action"))
                .andExpect(jsonPath("$[2].count").value(50))
        }

        @Test
        fun `returns empty list when no types`() {
            every { repository.findAllFoundryTypes() } returns emptyList()

            mockMvc.perform(get("/api/ingestion/types"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class GetStats {

        @Test
        fun `returns stats with counts`() {
            every { repository.count() } returns 100L
            every { repository.countPendingVectorization() } returns 30L

            mockMvc.perform(get("/api/ingestion/stats"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalEntries").value(100))
                .andExpect(jsonPath("$.vectorizedEntries").value(70))
                .andExpect(jsonPath("$.pendingEntries").value(30))
        }

        @Test
        fun `returns zero stats when empty`() {
            every { repository.count() } returns 0L
            every { repository.countPendingVectorization() } returns 0L

            mockMvc.perform(get("/api/ingestion/stats"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalEntries").value(0))
                .andExpect(jsonPath("$.vectorizedEntries").value(0))
                .andExpect(jsonPath("$.pendingEntries").value(0))
        }
    }
}
