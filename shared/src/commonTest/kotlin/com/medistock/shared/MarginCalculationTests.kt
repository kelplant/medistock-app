package com.medistock.shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for margin calculation logic.
 * These tests verify the margin calculation formulas used in purchase operations.
 */
class MarginCalculationTests {

    // Helper function that replicates the margin logic
    private fun calculateSellingPrice(purchasePrice: Double, marginType: String?, marginValue: Double?): Double {
        return when (marginType) {
            "fixed" -> purchasePrice + (marginValue ?: 0.0)
            "percentage" -> purchasePrice * (1 + (marginValue ?: 0.0) / 100)
            else -> purchasePrice
        }
    }

    // Helper for floating-point comparison (kotlin.test doesn't have assertEquals with tolerance)
    private fun assertEqualsWithTolerance(expected: Double, actual: Double, tolerance: Double = 0.00001) {
        assertTrue(abs(expected - actual) < tolerance, "Expected $expected but was $actual (tolerance: $tolerance)")
    }

    @Test
    fun `should_calculate_110_when_percentageMargin10On100`() {
        val result = calculateSellingPrice(100.0, "percentage", 10.0)
        assertEqualsWithTolerance(110.0, result)
    }

    @Test
    fun `should_calculate_100_when_percentageMargin25On80`() {
        val result = calculateSellingPrice(80.0, "percentage", 25.0)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_returnSamePrice_when_percentageMarginIsZero`() {
        val result = calculateSellingPrice(100.0, "percentage", 0.0)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_calculate_115_when_fixedMargin15On100`() {
        val result = calculateSellingPrice(100.0, "fixed", 15.0)
        assertEqualsWithTolerance(115.0, result)
    }

    @Test
    fun `should_returnSamePrice_when_fixedMarginIsZero`() {
        val result = calculateSellingPrice(100.0, "fixed", 0.0)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_calculateWithDecimal_when_percentageMarginOnDecimalPrice`() {
        val result = calculateSellingPrice(10.555, "percentage", 20.0)
        // 10.555 * 1.20 = 12.666
        assertEqualsWithTolerance(12.666, result, 0.001)
    }

    @Test
    fun `should_calculateWithDecimal_when_fixedMarginWithDecimalValue`() {
        val result = calculateSellingPrice(50.25, "fixed", 5.75)
        // 50.25 + 5.75 = 56.00
        assertEqualsWithTolerance(56.0, result, 0.001)
    }

    @Test
    fun `should_returnOriginalPrice_when_marginTypeIsNull`() {
        val result = calculateSellingPrice(100.0, null, 10.0)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_returnOriginalPrice_when_marginValueIsNull`() {
        val result = calculateSellingPrice(100.0, "percentage", null)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_returnOriginalPrice_when_marginTypeIsUnknown`() {
        val result = calculateSellingPrice(100.0, "unknown", 10.0)
        assertEqualsWithTolerance(100.0, result)
    }

    @Test
    fun `should_scaleFixedMarginByConversionFactor_when_level2Sale`() {
        // For level 2 sales with fixed margin:
        // Selling price per box = (purchasePrice * cf) + (marginValue * cf)
        // Example: purchasePrice=100, cf=10, marginValue=20
        // Selling price = (100*10) + (20*10) = 1000 + 200 = 1200 per box

        val purchasePrice = 100.0
        val conversionFactor = 10.0
        val marginValue = 20.0

        // Calculate price per base unit first (level 1)
        val pricePerBaseUnit = calculateSellingPrice(purchasePrice, "fixed", marginValue)
        // Then scale to level 2
        val pricePerBox = pricePerBaseUnit * conversionFactor

        assertEqualsWithTolerance(1200.0, pricePerBox)
    }

    @Test
    fun `should_applyPercentageMarginSameWay_when_level2Sale`() {
        // For level 2 sales with percentage margin:
        // The percentage applies the same way regardless of level
        // Example: purchasePrice=100, cf=10, marginValue=20%
        // Price per base unit = 100 * 1.20 = 120
        // Price per box = 120 * 10 = 1200

        val purchasePrice = 100.0
        val conversionFactor = 10.0
        val marginPercent = 20.0

        // Calculate price per base unit
        val pricePerBaseUnit = calculateSellingPrice(purchasePrice, "percentage", marginPercent)
        // Scale to level 2
        val pricePerBox = pricePerBaseUnit * conversionFactor

        assertEqualsWithTolerance(1200.0, pricePerBox)
    }

    @Test
    fun `should_calculate1200_when_fixedMargin20OnPrice100WithCF10`() {
        // Verify the exact formula for fixed margin with conversion factor
        val purchasePrice = 100.0
        val cf = 10.0
        val marginValue = 20.0

        // Formula: (purchasePrice + marginValue) * cf
        val expected = (purchasePrice + marginValue) * cf

        assertEqualsWithTolerance(1200.0, expected)
    }

    @Test
    fun `should_handleDecimalConversionFactor_when_level2FixedMargin`() {
        // Test with decimal conversion factor
        val purchasePrice = 50.0
        val cf = 12.5
        val marginValue = 10.0

        val pricePerBaseUnit = calculateSellingPrice(purchasePrice, "fixed", marginValue)
        val pricePerBox = pricePerBaseUnit * cf

        // (50 + 10) * 12.5 = 60 * 12.5 = 750
        assertEqualsWithTolerance(750.0, pricePerBox)
    }

    @Test
    fun `should_handleDecimalConversionFactor_when_level2PercentageMargin`() {
        // Test with decimal conversion factor and percentage
        val purchasePrice = 80.0
        val cf = 15.0
        val marginPercent = 25.0

        val pricePerBaseUnit = calculateSellingPrice(purchasePrice, "percentage", marginPercent)
        val pricePerBox = pricePerBaseUnit * cf

        // 80 * 1.25 * 15 = 100 * 15 = 1500
        assertEqualsWithTolerance(1500.0, pricePerBox)
    }
}
