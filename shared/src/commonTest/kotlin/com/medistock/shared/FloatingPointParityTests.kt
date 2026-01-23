package com.medistock.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for floating-point arithmetic consistency across platforms.
 * These tests ensure Android and iOS produce identical results for the same calculations.
 */
class FloatingPointParityTests {

    @Test
    fun divisionResultConsistency() {
        // 100 / 3 should give consistent result
        val result = 100.0 / 3.0
        // We expect approximately 33.333...
        assertTrue(result > 33.333 && result < 33.334)
        assertEquals(33.333333333333336, result)
    }

    @Test
    fun multiplicationPrecision() {
        // 0.1 * 0.2 is a classic floating-point case
        val result = 0.1 * 0.2
        // Should be very close to 0.02
        assertTrue(result > 0.019 && result < 0.021)
    }

    @Test
    fun sumOfFractions() {
        // 0.1 + 0.2 is famously not exactly 0.3 in IEEE 754
        val result = 0.1 + 0.2
        // We just verify it's close to 0.3
        assertTrue(result > 0.299 && result < 0.301)
    }

    @Test
    fun largeNumberMultiplication() {
        // Large number multiplication
        val a = 1_000_000_000.0
        val b = 1_000_000.0
        val result = a * b
        assertEquals(1_000_000_000_000_000.0, result)
    }

    @Test
    fun smallNumberDivision() {
        // Very small number division
        val a = 0.000001
        val b = 0.000001
        val result = a / b
        assertEquals(1.0, result)
    }

    @Test
    fun marginCalculationPrecision() {
        // Simulating margin calculation: 10.555 * 1.20
        val purchasePrice = 10.555
        val margin = 1.20
        val result = purchasePrice * margin
        // Should be approximately 12.666
        assertTrue(result > 12.665 && result < 12.667)
    }

    @Test
    fun costCalculationWithMultipleBatches() {
        // Simulate FIFO cost calculation
        // Batch 1: 50 units at 10.0
        // Batch 2: 30 units at 12.5
        // Total cost = 50*10 + 30*12.5 = 500 + 375 = 875
        val batch1Cost = 50.0 * 10.0
        val batch2Cost = 30.0 * 12.5
        val totalCost = batch1Cost + batch2Cost
        assertEquals(875.0, totalCost)
    }

    @Test
    fun averageCostCalculation() {
        // Average cost = 875 / 80 = 10.9375
        val totalCost = 875.0
        val totalQuantity = 80.0
        val averageCost = totalCost / totalQuantity
        assertEquals(10.9375, averageCost)
    }

    @Test
    fun profitCalculationWithDecimals() {
        // Revenue: 80 units at 15.99 = 1279.20
        // Cost: 875
        // Profit: 404.20
        val revenue = 80.0 * 15.99
        val cost = 875.0
        val profit = revenue - cost
        assertEquals(404.20, profit, 0.01)
    }

    @Test
    fun percentageMarginCalculation() {
        // 25% margin on 100.0
        val price = 100.0
        val marginPercent = 25.0
        val sellingPrice = price * (1 + marginPercent / 100)
        assertEquals(125.0, sellingPrice)
    }

    @Test
    fun fixedMarginCalculation() {
        // Fixed margin of 15.50 on 100.0
        val price = 100.0
        val marginFixed = 15.50
        val sellingPrice = price + marginFixed
        assertEquals(115.50, sellingPrice)
    }

    @Test
    fun quantityRemainingAfterMultipleSales() {
        // Initial: 100.0
        // Sale 1: 33.333
        // Sale 2: 33.333
        // Sale 3: 33.333
        // Remaining should be very close to 0.001
        var remaining = 100.0
        remaining -= 33.333
        remaining -= 33.333
        remaining -= 33.333
        assertTrue(remaining > 0.0 && remaining < 0.002)
    }
}
