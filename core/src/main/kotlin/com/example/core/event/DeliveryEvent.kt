package com.example.core.event

import java.time.LocalDateTime

data class DeliveryEvent(
    val deliveryId: String,
    val orderId: String,
    val address: String,
    val status: DeliveryStatus,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class DeliveryStatus {
    PENDING,
    IN_TRANSIT,
    DELIVERED,
    FAILED
}