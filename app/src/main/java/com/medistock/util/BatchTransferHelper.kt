package com.medistock.util

import com.medistock.data.dao.PurchaseBatchDao
import com.medistock.data.entities.PurchaseBatch
import kotlinx.coroutines.flow.first

/**
 * Helper class to manage FIFO batch transfers between sites.
 * When transferring products, batches are consumed in FIFO order (oldest first).
 */
class BatchTransferHelper(private val batchDao: PurchaseBatchDao) {

    /**
     * Transfer quantity from source site to destination site using FIFO.
     * Returns list of batch transfers (source batch info for creating destination batches).
     */
    suspend fun transferBatchesFIFO(
        productId: Long,
        fromSiteId: Long,
        toSiteId: Long,
        totalQuantity: Double,
        currentUser: String
    ): List<BatchTransferInfo> {
        val batches = batchDao.getAvailableBatchesFIFO(productId, fromSiteId).first()
        var remainingToTransfer = totalQuantity
        val transferInfoList = mutableListOf<BatchTransferInfo>()

        for (batch in batches) {
            if (remainingToTransfer <= 0) break

            val quantityFromThisBatch = minOf(batch.remainingQuantity, remainingToTransfer)

            // Update source batch
            val updatedBatch = batch.copy(
                remainingQuantity = batch.remainingQuantity - quantityFromThisBatch,
                isExhausted = (batch.remainingQuantity - quantityFromThisBatch) <= 0.0,
                updatedAt = System.currentTimeMillis(),
                updatedBy = currentUser
            )
            batchDao.update(updatedBatch)

            // Create new batch on destination site
            val newBatch = PurchaseBatch(
                productId = productId,
                siteId = toSiteId,
                batchNumber = "${batch.batchNumber}-TRANSFER",
                purchaseDate = batch.purchaseDate, // Keep original purchase date for FIFO
                initialQuantity = quantityFromThisBatch,
                remainingQuantity = quantityFromThisBatch,
                purchasePrice = batch.purchasePrice,
                supplierName = "Transfer from Site ${fromSiteId}",
                expiryDate = batch.expiryDate,
                isExhausted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                createdBy = currentUser,
                updatedBy = currentUser
            )
            batchDao.insert(newBatch)

            // Track transfer info
            transferInfoList.add(
                BatchTransferInfo(
                    batchNumber = batch.batchNumber,
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
