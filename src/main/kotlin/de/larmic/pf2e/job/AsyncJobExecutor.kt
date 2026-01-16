package de.larmic.pf2e.job

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Managed coroutine executor for async job processing.
 *
 * This component provides a properly lifecycle-managed CoroutineScope
 * that is cancelled when the Spring application context shuts down,
 * preventing memory leaks from orphaned coroutines.
 *
 * Uses SupervisorJob to ensure that failure of one job doesn't
 * cancel other running jobs.
 */
@Component
class AsyncJobExecutor {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Execute a suspending block asynchronously.
     *
     * Failures in the block are logged but don't affect other jobs.
     */
    fun execute(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                logger.error("Async job failed", e)
                throw e
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down AsyncJobExecutor, cancelling all running jobs")
        scope.cancel("Application shutdown")
    }
}
