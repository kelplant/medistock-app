package com.medistock.util

import com.medistock.shared.data.repository.PurchaseBatchRepository
import com.medistock.shared.domain.model.PurchaseBatch
import java.util.UUID

/**
 * Helper class to manage FIFO batch transfers between sites.
 * When transferring products, batches are consumed in FIFO order (oldest first).
 */
class BatchTransferHelper(private val batchRepository: PurchaseBatchRepository) {

    /**
     * Transfer quantity from source site to destination site using FIFO.
     * Returns list of batch transfers (source batch info for creating destination batches).
     */
    suspend fun transferBatchesFIFO(
        productId: String,
        fromSiteId: String,
        toSiteId: String,
        totalQuantity: Double,
        currentUser: String
    ): List<BatchTransferInfo> {
        val batches = batchRepository.getByProductAndSite(productId, fromSiteId)
            .filter { !it.isExhausted && it.remainingQuantity > 0 }
            .sortedBy { it.purchaseDate }
        var remainingToTransfer = totalQuantity
        val transferInfoList = mutableListOf<BatchTransferInfo>()
        val now = System.currentTimeMillis()

        for (batch in batches) {
            if (remainingToTransfer <= 0) break

            val quantityFromThisBatch = minOf(batch.remainingQuantity, remainingToTransfer)

            // Update source batch
            val newRemainingQty = batch.remainingQuantity - quantityFromThisBatch
            val isExhausted = newRemainingQty <= 0.0
            batchRepository.updateQuantity(batch.id, newRemainingQty, isExhausted, now, currentUser)

            // Create new batch on destination site
            val newBatch = PurchaseBatch(
                id = UUID.randomUUID().toString(),
                productId = productId,
                siteId = toSiteId,
                batchNumber = "${batch.batchNumber ?: "Batch-${batch.id}"}-TRANSFER",
                purchaseDate = batch.purchaseDate, // Keep original purchase date for FIFO
                initialQuantity = quantityFromThisBatch,
                remainingQuantity = quantityFromThisBatch,
                purchasePrice = batch.purchasePrice,
                supplierName = "Transfer from Site ${fromSiteId}",
                expiryDate = batch.expiryDate,
                isExhausted = false,
                createdAt = now,
                updatedAt = now,
                createdBy = currentUser,
                updatedBy = currentUser
            )
            batchRepository.insert(newBatch)

            // Track transfer info
            transferInfoList.add(
                BatchTransferInfo(
                    batchNumber = batch.batchNumber ?: "Batch-${batch.id}",
                    quantityTransferred = quantityFromThisBatch,
                    purchasePrice = batch.purchasePrice,
                    expiryDate = batch.expiryDate
                )
            )

            remainingToTransfer -= quantityFromThisBatch
        }

        if (remainingToTransfer > 0) {
            throw InsufficientStockException(
                "Not enough stock to transfer. Missing: $remainingToTransfer"
            )
        }

        return transferInfoList
    }

    /**
     * Calculate average purchase price for transferred batches.
     */
    fun calculateAveragePurchasePrice(transfers: List<BatchTransferInfo>): Double {
        if (transfers.isEmpty()) return 0.0
        val totalQuantity = transfers.sumOf { it.quantityTransferred }
        val weightedSum = transfers.sumOf { it.quantityTransferred * it.purchasePrice }
        return weightedSum / totalQuantity
    }
}

/**
 * Information about a batch transfer for audit and tracking.
 */
data class BatchTransferInfo(
    val batchNumber: String,
    val quantityTransferred: Double,
    val purchasePrice: Double,
    val expiryDate: Long?
)

/**
 * Exception thrown when there is insufficient stock for a transfer.
 */
class InsufficientStockException(message: String) : Exception(message)
