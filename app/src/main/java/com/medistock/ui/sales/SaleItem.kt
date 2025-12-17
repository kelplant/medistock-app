package com.medistock.ui.sales

data class SaleItem(
    val productId: Long,
    val productName: String,
    val unit: String,
    var quantity: Double,
    var pricePerUnit: Double
) {
    fun getSubtotal(): Double = quantity * pricePerUnit
}
