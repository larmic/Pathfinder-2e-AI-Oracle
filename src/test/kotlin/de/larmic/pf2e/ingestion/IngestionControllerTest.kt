package de.larmic.pf2e.ingestion

import com.ninjasquad.springmockk.MockkBean
import de.larmic.pf2e.domain.FoundryRawEntryRepository
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(IngestionController::class)
class IngestionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var ingestionService: IngestionService

    @MockkBean
    private lateinit var repository: FoundryRawEntryRepository

    @Nested
    inner class PostIngestByType {

        @Test
        fun `returns 200 OK with ingestion result`() {
            every { ingestionService.ingestByType("spell") } returns IngestionResult(
                processed = 10,
                errors = 0,
                total = 10,
                durationSeconds = 5
            )

            mockMvc.perform(post("/api/ingestion/type/spell"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processed").value(10))
                .andExpect(jsonPath("$.errors").value(0))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.durationSeconds").value(5))
        }

        @Test
        fun `calls service with correct type`() {
            every { ingestionService.ingestByType("feat") } returns IngestionResult(0, 0, 0, 0)

            mockMvc.perform(post("/api/ingestion/type/feat"))
                .andExpect(status().isOk)

            verify { ingestionService.ingestByType("feat") }
        }

        @Test
        fun `returns result with errors`() {
            every { ingestionService.ingestByType("action") } returns IngestionResult(
                processed = 8,
                errors = 2,
                total = 10,
                durationSeconds = 3
            )

            mockMvc.perform(post("/api/ingestion/type/action"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.processed").value(8))
                .andExpect(jsonPath("$.errors").value(2))
        }
    }

    @Nested
    inner class PostIngestAll {

        @Test
        fun `returns 200 OK with ingestion result`() {
            every { ingestionService.ingestAll() } returns IngestionResult(
                processed = 1000,
                errors = 5,
                total = 1005,
                durationSeconds = 120
            )

            mockMvc.perform(post("/api/ingestion/all"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.processed").value(1000))
                .andExpect(jsonPath("$.errors").value(5))
                .andExpect(jsonPath("$.total").value(1005))
                .andExpect(jsonPath("$.durationSeconds").value(120))
        }

        @Test
        fun `calls ingestAll service method`() {
            every { ingestionService.ingestAll() } returns IngestionResult(0, 0, 0, 0)

            mockMvc.perform(post("/api/ingestion/all"))
                .andExpect(status().isOk)

            verify { ingestionService.ingestAll() }
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
}
