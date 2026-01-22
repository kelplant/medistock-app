package com.medistock.shared.domain.sync

import kotlinx.coroutines.delay

/**
 * Configuration for exponential backoff retry strategy.
 * Shared between Android and iOS for consistent sync behavior.
 */
data class RetryConfiguration(
    val maxRetries: Int = 5,
    val backoffDelaysMs: List<Long> = listOf(1000L, 2000L, 4000L, 8000L, 16000L),
    val batchSize: Int = 10,
    val syncIntervalMs: Long = 30000L
) {
    /**
     * Get the delay for a given retry attempt.
     * @param retryCount Current retry count (0-indexed)
     * @return Delay in milliseconds
     */
    fun getDelayMs(retryCount: Int): Long {
        return backoffDelaysMs.getOrElse(retryCount) { backoffDelaysMs.last() }
    }

    /**
     * Get the delay in seconds for a given retry attempt.
     * @param retryCount Current retry count (0-indexed)
     * @return Delay in seconds
     */
    fun getDelaySeconds(retryCount: Int): Double {
        return getDelayMs(retryCount) / 1000.0
    }

    /**
     * Check if we should retry based on current retry count.
     */
    fun shouldRetry(retryCount: Int): Boolean = retryCount < maxRetries

    companion object {
        /**
         * Default configuration instance.
         */
        val DEFAULT = RetryConfiguration()
    }
}

/**
 * Result of a retry operation.
 */
sealed class RetryResult<out T> {
    data class Success<T>(val value: T) : RetryResult<T>()
    data class Failure(val error: Throwable, val attempts: Int) : RetryResult<Nothing>()
}

/**
 * Executes an operation with exponential backoff retry.
 *
 * @param config Retry configuration
 * @param operation The operation to execute, receives the current attempt number (0-indexed)
 * @return Result of the operation
 */
suspend fun <T> withRetry(
    config: RetryConfiguration = RetryConfiguration.DEFAULT,
    operation: suspend (attempt: Int) -> T
): RetryResult<T> {
    var lastError: Throwable? = null

    repeat(config.maxRetries) { attempt ->
        try {
            return RetryResult.Success(operation(attempt))
        } catch (e: Exception) {
            lastError = e
            if (attempt < config.maxRetries - 1) {
                delay(config.getDelayMs(attempt))
            }
        }
    }

    return RetryResult.Failure(
        error = lastError ?: Exception("Max retries exceeded"),
        attempts = config.maxRetries
    )
}

/**
 * Executes an operation with exponential backoff retry, with callbacks.
 *
 * @param config Retry configuration
 * @param onRetry Called before each retry with attempt number and error
 * @param operation The operation to execute
 * @return Result of the operation
 */
suspend fun <T> withRetryAndCallback(
    config: RetryConfiguration = RetryConfiguration.DEFAULT,
    onRetry: suspend (attempt: Int, error: Throwable) -> Unit = { _, _ -> },
    operation: suspend (attempt: Int) -> T
): RetryResult<T> {
    var lastError: Throwable? = null

    repeat(config.maxRetries) { attempt ->
        try {
            return RetryResult.Success(operation(attempt))
        } catch (e: Exception) {
            lastError = e
            if (attempt < config.maxRetries - 1) {
                onRetry(attempt, e)
                delay(config.getDelayMs(attempt))
            }
        }
    }

    return RetryResult.Failure(
        error = lastError ?: Exception("Max retries exceeded"),
        attempts = config.maxRetries
    )
}
