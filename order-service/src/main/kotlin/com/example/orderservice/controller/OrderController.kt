package com.example.orderservice.controller

import com.example.core.event.OrderEvent
import com.example.core.event.OrderItem
import com.example.orderservice.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
        @RequestParam(defaultValue = "standard") queueType: String
    ): ResponseEntity<OrderEvent> {
        val orderEvent = OrderEvent(
            orderId = UUID.randomUUID().toString(),
            customerId = request.customerId,
            items = request.items,
            totalAmount = request.items.sumOf { it.price * it.quantity },
            timestamp = LocalDateTime.now()
        )
        
        val createdOrder = orderService.createOrder(orderEvent, queueType)
        return ResponseEntity.ok(createdOrder)
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP", "service" to "order-service"))
    }
}

data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItem>
)
