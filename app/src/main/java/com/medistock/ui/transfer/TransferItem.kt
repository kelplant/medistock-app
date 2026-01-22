package com.medistock.ui.transfer

data class TransferItem(
    val productId: String,
    val productName: String,
    val unit: String,
    var quantity: Double
)
