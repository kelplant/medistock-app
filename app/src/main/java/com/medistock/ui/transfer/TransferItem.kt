package com.medistock.ui.transfer

data class TransferItem(
    val productId: Long,
    val productName: String,
    val unit: String,
    var quantity: Double
)
