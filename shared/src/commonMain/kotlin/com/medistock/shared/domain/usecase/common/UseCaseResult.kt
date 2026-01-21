package com.medistock.shared.domain.usecase.common

/**
 * Represents the result of a UseCase operation.
 * Can contain warnings (non-blocking issues) alongside the result.
 */
sealed class UseCaseResult<out T> {
    /**
     * Successful result with optional warnings
     */
    data class Success<T>(
        val data: T,
        val warnings: List<BusinessWarning> = emptyList()
    ) : UseCaseResult<T>()

    /**
     * Failed result due to validation or business rule violation
     */
    data class Error(
        val error: BusinessError
    ) : UseCaseResult<Nothing>()
}

/**
 * Non-blocking warning that doesn't prevent the operation
 * Examples: insufficient stock (allowed per business rules)
 */
sealed class BusinessWarning {
    /**
     * Stock is insufficient but operation proceeds (negative stock allowed)
     */
    data class InsufficientStock(
        val productId: String,
        val productName: String? = null,
        val siteId: String,
        val requested: Double,
        val available: Double
    ) : BusinessWarning() {
        val shortage: Double get() = requested - available
    }

    /**
     * Product is expiring soon
     */
    data class ExpiringProduct(
        val productId: String,
        val productName: String? = null,
        val batchId: String,
        val expiryDate: Long,
        val daysUntilExpiry: Int
    ) : BusinessWarning()

    /**
     * Stock is below minimum threshold
     */
    data class LowStock(
        val productId: String,
        val productName: String? = null,
        val siteId: String,
        val currentStock: Double,
        val minStock: Double
    ) : BusinessWarning()
}

/**
 * Blocking error that prevents the operation
 */
sealed class BusinessError {
    abstract val message: String

    /**
     * Required field is missing or empty
     */
    data class ValidationError(
        val field: String,
        override val message: String
    ) : BusinessError()

    /**
     * Entity not found in database
     */
    data class NotFound(
        val entityType: String,
        val entityId: String,
        override val message: String = "$entityType with id $entityId not found"
    ) : BusinessError()

    /**
     * Transfer to same site
     */
    data class SameSiteTransfer(
        val siteId: String,
        override val message: String = "Cannot transfer to the same site"
    ) : BusinessError()

    /**
     * Generic business rule violation
     */
    data class BusinessRuleViolation(
        override val message: String
    ) : BusinessError()

    /**
     * Database or system error
     */
    data class SystemError(
        override val message: String,
        val cause: Throwable? = null
    ) : BusinessError()
}

/**
 * Extension to check if result has warnings
 */
fun <T> UseCaseResult<T>.hasWarnings(): Boolean {
    return this is UseCaseResult.Success && this.warnings.isNotEmpty()
}

/**
 * Extension to get data or throw
 */
fun <T> UseCaseResult<T>.getOrThrow(): T {
    return when (this) {
        is UseCaseResult.Success -> this.data
        is UseCaseResult.Error -> throw IllegalStateException(this.error.message)
    }
}

/**
 * Extension to map successful result
 */
inline fun <T, R> UseCaseResult<T>.map(transform: (T) -> R): UseCaseResult<R> {
    return when (this) {
        is UseCaseResult.Success -> UseCaseResult.Success(transform(this.data), this.warnings)
        is UseCaseResult.Error -> this
    }
}
