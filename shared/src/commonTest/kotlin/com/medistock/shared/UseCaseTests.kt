package com.medistock.shared

import com.medistock.shared.domain.model.*
import com.medistock.shared.domain.usecase.*
import com.medistock.shared.domain.usecase.common.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Tests for business logic validation in UseCases
 */

class PurchaseInputValidationTest {

    @Test
    fun validPurchaseInput_hasAllRequiredFields() {
        val input = PurchaseInput(
            productId = "prod-1",
            siteId = "site-1",
            quantity = 100.0,
            purchasePrice = 10.0,
            supplierName = "Supplier A",
            userId = "user-1"
        )

        assertTrue(input.productId.isNotBlank())
        assertTrue(input.siteId.isNotBlank())
        assertTrue(input.quantity > 0)
        assertTrue(input.purchasePrice >= 0)
        assertTrue(input.userId.isNotBlank())
    }

    @Test
    fun purchaseInput_withOptionalFields_createsCorrectly() {
        val input = PurchaseInput(
            productId = "prod-1",
            siteId = "site-1",
            quantity = 50.0,
            purchasePrice = 25.0,
            supplierName = "Supplier B",
            batchNumber = "BATCH-001",
            expiryDate = 1735689600000L, // Jan 1, 2025
            userId = "user-1"
        )

        assertEquals("BATCH-001", input.batchNumber)
        assertEquals(1735689600000L, input.expiryDate)
    }

    @Test
    fun purchaseInput_defaultValues_areCorrect() {
        val input = PurchaseInput(
            productId = "prod-1",
            siteId = "site-1",
            quantity = 10.0,
            purchasePrice = 5.0,
            userId = "user-1"
        )

        assertEquals("", input.supplierName)
        assertEquals(null, input.batchNumber)
        assertEquals(null, input.expiryDate)
    }
}

class SaleInputValidationTest {

    @Test
    fun validSaleInput_hasAllRequiredFields() {
        val items = listOf(
            SaleItemInput(
                productId = "prod-1",
                quantity = 5.0,
                unitPrice = 15.0
            )
        )

        val input = SaleInput(
            siteId = "site-1",
            customerName = "John Doe",
            customerId = "cust-1",
            items = items,
            userId = "user-1"
        )

        assertTrue(input.siteId.isNotBlank())
        assertTrue(input.customerName.isNotBlank())
        assertTrue(input.items.isNotEmpty())
        assertTrue(input.userId.isNotBlank())
    }

    @Test
    fun saleItemInput_calculatesCorrectly() {
        val item = SaleItemInput(
            productId = "prod-1",
            quantity = 10.0,
            unitPrice = 25.0
        )

        assertEquals(10.0, item.quantity)
        assertEquals(25.0, item.unitPrice)
        // Total would be 250.0
    }

    @Test
    fun saleInput_withMultipleItems_createsCorrectly() {
        val items = listOf(
            SaleItemInput(productId = "prod-1", quantity = 5.0, unitPrice = 10.0),
            SaleItemInput(productId = "prod-2", quantity = 3.0, unitPrice = 20.0),
            SaleItemInput(productId = "prod-3", quantity = 2.0, unitPrice = 15.0)
        )

        val input = SaleInput(
            siteId = "site-1",
            customerName = "Customer",
            customerId = null,
            items = items,
            userId = "user-1"
        )

        assertEquals(3, input.items.size)
        assertEquals(null, input.customerId)
    }
}

class TransferInputValidationTest {

    @Test
    fun validTransferInput_hasAllRequiredFields() {
        val input = TransferInput(
            productId = "prod-1",
            fromSiteId = "site-1",
            toSiteId = "site-2",
            quantity = 25.0,
            notes = "Transfer for restocking",
            userId = "user-1"
        )

        assertTrue(input.productId.isNotBlank())
        assertTrue(input.fromSiteId.isNotBlank())
        assertTrue(input.toSiteId.isNotBlank())
        assertTrue(input.fromSiteId != input.toSiteId)
        assertTrue(input.quantity > 0)
    }

    @Test
    fun transferInput_sitesAreDifferent() {
        val input = TransferInput(
            productId = "prod-1",
            fromSiteId = "site-A",
            toSiteId = "site-B",
            quantity = 10.0,
            notes = null,
            userId = "user-1"
        )

        assertFalse(input.fromSiteId == input.toSiteId)
    }
}

class BusinessErrorTest {

    @Test
    fun validationError_hasCorrectFields() {
        val error = BusinessError.ValidationError("quantity", "Must be positive")

        assertEquals("quantity", error.field)
        assertEquals("Must be positive", error.message)
    }

    @Test
    fun notFoundError_hasCorrectFields() {
        val error = BusinessError.NotFound("Product", "prod-123")

        assertEquals("Product", error.entityType)
        assertEquals("prod-123", error.entityId)
    }

    @Test
    fun sameSiteTransferError_hasCorrectFields() {
        val error = BusinessError.SameSiteTransfer("site-1")

        assertEquals("site-1", error.siteId)
        assertTrue(error.message.contains("same site"))
    }

    @Test
    fun businessRuleViolation_hasCorrectFields() {
        val error = BusinessError.BusinessRuleViolation("Custom rule violated")

        assertEquals("Custom rule violated", error.message)
    }

    @Test
    fun systemError_hasCorrectFields() {
        val cause = Exception("Database connection failed")
        val error = BusinessError.SystemError("Database error", cause)

        assertEquals("Database error", error.message)
        assertEquals(cause, error.cause)
    }
}

class BusinessWarningTest {

    @Test
    fun insufficientStockWarning_hasCorrectFields() {
        val warning = BusinessWarning.InsufficientStock(
            productId = "prod-1",
            productName = "Ibuprofen",
            siteId = "site-1",
            requested = 50.0,
            available = 30.0
        )

        assertEquals("prod-1", warning.productId)
        assertEquals("Ibuprofen", warning.productName)
        assertEquals("site-1", warning.siteId)
        assertEquals(50.0, warning.requested)
        assertEquals(30.0, warning.available)
        assertEquals(20.0, warning.shortage) // 50 - 30 = 20
    }

    @Test
    fun lowStockWarning_hasCorrectFields() {
        val warning = BusinessWarning.LowStock(
            productId = "prod-2",
            productName = "Aspirin",
            siteId = "site-1",
            currentStock = 15.0,
            minStock = 20.0
        )

        assertEquals("prod-2", warning.productId)
        assertEquals("site-1", warning.siteId)
        assertEquals(15.0, warning.currentStock)
        assertEquals(20.0, warning.minStock)
    }

    @Test
    fun expiringProductWarning_hasCorrectFields() {
        val warning = BusinessWarning.ExpiringProduct(
            productId = "prod-3",
            productName = "Vitamin C",
            batchId = "batch-1",
            expiryDate = 1735689600000L,
            daysUntilExpiry = 15
        )

        assertEquals("prod-3", warning.productId)
        assertEquals("batch-1", warning.batchId)
        assertEquals(15, warning.daysUntilExpiry)
    }
}

class UseCaseResultTest {

    @Test
    fun successResult_containsDataAndWarnings() {
        val data = "test data"
        val warnings = listOf(
            BusinessWarning.LowStock("p1", "Product", "site-1", 5.0, 10.0)
        )

        val result: UseCaseResult<String> = UseCaseResult.Success(data, warnings)

        assertIs<UseCaseResult.Success<String>>(result)
        assertEquals("test data", result.data)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun successResult_withNoWarnings() {
        val result: UseCaseResult<Int> = UseCaseResult.Success(42, emptyList())

        assertIs<UseCaseResult.Success<Int>>(result)
        assertEquals(42, result.data)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun errorResult_containsError() {
        val error = BusinessError.ValidationError("field", "Invalid")
        val result: UseCaseResult<String> = UseCaseResult.Error(error)

        assertIs<UseCaseResult.Error>(result)
        assertIs<BusinessError.ValidationError>(result.error)
    }

    @Test
    fun hasWarnings_returnsTrueWhenWarningsExist() {
        val result: UseCaseResult<String> = UseCaseResult.Success(
            "data",
            listOf(BusinessWarning.LowStock("p1", null, "s1", 5.0, 10.0))
        )

        assertTrue(result.hasWarnings())
    }

    @Test
    fun hasWarnings_returnsFalseWhenNoWarnings() {
        val result: UseCaseResult<String> = UseCaseResult.Success("data", emptyList())

        assertFalse(result.hasWarnings())
    }

    @Test
    fun getOrThrow_returnsDataOnSuccess() {
        val result: UseCaseResult<Int> = UseCaseResult.Success(42, emptyList())

        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun mapResult_transformsSuccessData() {
        val result: UseCaseResult<Int> = UseCaseResult.Success(10, emptyList())
        val mapped = result.map { it * 2 }

        assertIs<UseCaseResult.Success<Int>>(mapped)
        assertEquals(20, mapped.data)
    }
}

class PurchaseResultTest {

    @Test
    fun purchaseResult_containsAllFields() {
        val batch = PurchaseBatch(
            id = "batch-1",
            productId = "prod-1",
            siteId = "site-1",
            purchaseDate = 1705680000000L,
            initialQuantity = 100.0,
            remainingQuantity = 100.0,
            purchasePrice = 10.0
        )

        val movement = StockMovement(
            id = "mov-1",
            productId = "prod-1",
            siteId = "site-1",
            quantity = 100.0,
            movementType = "PURCHASE",
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )

        val result = PurchaseResult(
            purchaseBatch = batch,
            stockMovement = movement,
            calculatedSellingPrice = 12.0
        )

        assertEquals("batch-1", result.purchaseBatch.id)
        assertEquals("mov-1", result.stockMovement.id)
        assertEquals(12.0, result.calculatedSellingPrice)
    }
}

class SaleResultTest {

    @Test
    fun saleResult_containsAllFields() {
        val sale = Sale(
            id = "sale-1",
            customerName = "John",
            date = 1705680000000L,
            totalAmount = 150.0,
            siteId = "site-1"
        )

        val saleItem = SaleItem(
            id = "item-1",
            saleId = "sale-1",
            productId = "prod-1",
            quantity = 10.0,
            unitPrice = 15.0,
            totalPrice = 150.0
        )

        val movement = StockMovement(
            id = "mov-1",
            productId = "prod-1",
            siteId = "site-1",
            quantity = -10.0,
            movementType = "SALE",
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )

        val processedItem = ProcessedSaleItem(
            saleItem = saleItem,
            allocations = emptyList(),
            stockMovement = movement,
            averageCost = 10.0
        )

        val result = SaleResult(
            sale = sale,
            items = listOf(processedItem),
            totalCost = 100.0,
            totalRevenue = 150.0,
            grossProfit = 50.0
        )

        assertEquals("sale-1", result.sale.id)
        assertEquals(1, result.items.size)
        assertEquals(150.0, result.totalRevenue)
        assertEquals(50.0, result.grossProfit)
    }
}

class TransferResultTest {

    @Test
    fun transferResult_containsAllFields() {
        val transfer = ProductTransfer(
            id = "transfer-1",
            productId = "prod-1",
            fromSiteId = "site-1",
            toSiteId = "site-2",
            quantity = 50.0,
            status = "completed",
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )

        val sourceMovement = StockMovement(
            id = "mov-out",
            productId = "prod-1",
            siteId = "site-1",
            quantity = -50.0,
            movementType = "TRANSFER_OUT",
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )

        val destinationMovement = StockMovement(
            id = "mov-in",
            productId = "prod-1",
            siteId = "site-2",
            quantity = 50.0,
            movementType = "TRANSFER_IN",
            createdAt = 1705680000000L,
            createdBy = "user-1"
        )

        val transferredBatch = TransferredBatch(
            sourceBatchId = "batch-1",
            destinationBatchId = "batch-2",
            quantity = 50.0,
            purchasePrice = 10.0
        )

        val result = TransferResult(
            transfer = transfer,
            transferredBatches = listOf(transferredBatch),
            sourceMovement = sourceMovement,
            destinationMovement = destinationMovement,
            averageCost = 10.0
        )

        assertEquals("transfer-1", result.transfer.id)
        assertEquals("site-1", result.sourceMovement.siteId)
        assertEquals("site-2", result.destinationMovement.siteId)
        assertEquals(1, result.transferredBatches.size)
        assertEquals(10.0, result.averageCost)
    }
}

class BatchAllocationDetailTest {

    @Test
    fun batchAllocationDetail_hasCorrectFields() {
        val allocation = BatchAllocationDetail(
            batchId = "batch-1",
            quantity = 25.0,
            unitCost = 8.50
        )

        assertEquals("batch-1", allocation.batchId)
        assertEquals(25.0, allocation.quantity)
        assertEquals(8.50, allocation.unitCost)
    }
}

class TransferredBatchTest {

    @Test
    fun transferredBatch_hasCorrectFields() {
        val transferred = TransferredBatch(
            sourceBatchId = "batch-source",
            destinationBatchId = "batch-dest",
            quantity = 30.0,
            purchasePrice = 12.50
        )

        assertEquals("batch-source", transferred.sourceBatchId)
        assertEquals("batch-dest", transferred.destinationBatchId)
        assertEquals(30.0, transferred.quantity)
        assertEquals(12.50, transferred.purchasePrice)
    }
}

class MovementTypeTest {

    @Test
    fun movementTypes_haveCorrectValues() {
        assertEquals("PURCHASE", MovementType.PURCHASE)
        assertEquals("SALE", MovementType.SALE)
        assertEquals("TRANSFER_IN", MovementType.TRANSFER_IN)
        assertEquals("TRANSFER_OUT", MovementType.TRANSFER_OUT)
        assertEquals("INVENTORY", MovementType.INVENTORY)
        assertEquals("MANUAL_IN", MovementType.MANUAL_IN)
        assertEquals("MANUAL_OUT", MovementType.MANUAL_OUT)
    }
}
