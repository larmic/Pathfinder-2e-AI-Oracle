package de.larmic.pf2e.importer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class ImportJobStoreTest {

    private lateinit var store: ImportJobStore

    @BeforeEach
    fun setUp() {
        store = ImportJobStore()
    }

    @Nested
    inner class Create {

        @Test
        fun `creates job with unique id`() {
            val job1 = store.create("feats")
            val job2 = store.create("spells")

            assertThat(job1.id).isNotEqualTo(job2.id)
        }

        @Test
        fun `creates job with PENDING status`() {
            val job = store.create("feats")

            assertThat(job.status).isEqualTo(JobStatus.PENDING)
        }

        @Test
        fun `creates job with specified itemType`() {
            val job = store.create("actions")

            assertThat(job.itemType).isEqualTo("actions")
        }

        @Test
        fun `stores job for later retrieval`() {
            val job = store.create("feats")

            assertThat(store.findById(job.id)).isEqualTo(job)
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `returns job when exists`() {
            val created = store.create("feats")

            val found = store.findById(created.id)

            assertThat(found).isNotNull
            assertThat(found?.itemType).isEqualTo("feats")
        }

        @Test
        fun `returns null when job does not exist`() {
            val result = store.findById(UUID.randomUUID())

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class FindAll {

        @Test
        fun `returns empty list when no jobs`() {
            val result = store.findAll()

            assertThat(result).isEmpty()
        }

        @Test
        fun `returns all created jobs`() {
            store.create("feats")
            store.create("spells")
            store.create("actions")

            val result = store.findAll()

            assertThat(result).hasSize(3)
        }
    }

    @Nested
    inner class Start {

        @Test
        fun `transitions job from PENDING to RUNNING`() {
            val job = store.create("feats")

            val started = store.start(job.id, 100)

            assertThat(started?.status).isEqualTo(JobStatus.RUNNING)
        }

        @Test
        fun `sets startedAt timestamp`() {
            val job = store.create("feats")
            assertThat(job.startedAt).isNull()

            val started = store.start(job.id, 100)

            assertThat(started?.startedAt).isNotNull
        }

        @Test
        fun `sets totalFiles in progress`() {
            val job = store.create("feats")

            val started = store.start(job.id, 500)

            assertThat(started?.progress?.totalFiles).isEqualTo(500)
        }

        @Test
        fun `returns null for non-existent job`() {
            val result = store.start(UUID.randomUUID(), 100)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class UpdateProgress {

        @Test
        fun `updates processedFiles count`() {
            val job = store.create("feats")
            store.start(job.id, 100)

            val updated = store.updateProgress(job.id, 50, 10)

            assertThat(updated?.progress?.processedFiles).isEqualTo(50)
        }

        @Test
        fun `updates skippedFiles count`() {
            val job = store.create("feats")
            store.start(job.id, 100)

            val updated = store.updateProgress(job.id, 50, 25)

            assertThat(updated?.progress?.skippedFiles).isEqualTo(25)
        }

        @Test
        fun `returns null for non-existent job`() {
            val result = store.updateProgress(UUID.randomUUID(), 50, 10)

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class Complete {

        @Test
        fun `transitions job to COMPLETED status`() {
            val job = store.create("feats")
            store.start(job.id, 100)
            val result = ImportResult(imported = 90, skipped = 10, errors = 0, durationSeconds = 60, totalFiles = 100)

            val completed = store.complete(job.id, result)

            assertThat(completed?.status).isEqualTo(JobStatus.COMPLETED)
        }

        @Test
        fun `sets completedAt timestamp`() {
            val job = store.create("feats")
            store.start(job.id, 100)
            val result = ImportResult(imported = 90, skipped = 10, errors = 0, durationSeconds = 60, totalFiles = 100)

            val completed = store.complete(job.id, result)

            assertThat(completed?.completedAt).isNotNull
        }

        @Test
        fun `stores the import result`() {
            val job = store.create("feats")
            store.start(job.id, 100)
            val result = ImportResult(imported = 90, skipped = 10, errors = 5, durationSeconds = 120, totalFiles = 100)

            val completed = store.complete(job.id, result)

            assertThat(completed?.result).isEqualTo(result)
            assertThat(completed?.result?.imported).isEqualTo(90)
            assertThat(completed?.result?.errors).isEqualTo(5)
        }

        @Test
        fun `returns null for non-existent job`() {
            val result = ImportResult(imported = 90, skipped = 10, errors = 0, durationSeconds = 60, totalFiles = 100)

            val completed = store.complete(UUID.randomUUID(), result)

            assertThat(completed).isNull()
        }
    }

    @Nested
    inner class Fail {

        @Test
        fun `transitions job to FAILED status`() {
            val job = store.create("feats")
            store.start(job.id, 100)

            val failed = store.fail(job.id, "Connection timeout")

            assertThat(failed?.status).isEqualTo(JobStatus.FAILED)
        }

        @Test
        fun `sets completedAt timestamp`() {
            val job = store.create("feats")
            store.start(job.id, 100)

            val failed = store.fail(job.id, "Error")

            assertThat(failed?.completedAt).isNotNull
        }

        @Test
        fun `stores error message`() {
            val job = store.create("feats")
            store.start(job.id, 100)

            val failed = store.fail(job.id, "GitHub API rate limit exceeded")

            assertThat(failed?.errorMessage).isEqualTo("GitHub API rate limit exceeded")
        }

        @Test
        fun `returns null for non-existent job`() {
            val result = store.fail(UUID.randomUUID(), "Error")

            assertThat(result).isNull()
        }
    }

    @Nested
    inner class Clear {

        @Test
        fun `removes all jobs`() {
            store.create("feats")
            store.create("spells")
            assertThat(store.findAll()).hasSize(2)

            store.clear()

            assertThat(store.findAll()).isEmpty()
        }
    }

    @Nested
    inner class ImportProgressPercentComplete {

        @Test
        fun `calculates percentage correctly`() {
            val progress = ImportProgress(totalFiles = 100, processedFiles = 50, skippedFiles = 0)

            assertThat(progress.percentComplete).isEqualTo(50)
        }

        @Test
        fun `returns 0 when totalFiles is 0`() {
            val progress = ImportProgress(totalFiles = 0, processedFiles = 0, skippedFiles = 0)

            assertThat(progress.percentComplete).isEqualTo(0)
        }

        @Test
        fun `returns 100 when all files processed`() {
            val progress = ImportProgress(totalFiles = 100, processedFiles = 100, skippedFiles = 0)

            assertThat(progress.percentComplete).isEqualTo(100)
        }
    }
}
