package com.example.core.event

import java.time.LocalDateTime

data class OrderEvent(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val price: Double
)