package com.example.deliveryservice.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/delivery")
class DeliveryController {

    @GetMapping("/status")
    fun getStatus(): Map<String, String> {
        return mapOf(
            "service" to "delivery-service",
            "status" to "running"
        )
    }

    @GetMapping("/health")
    fun healthCheck(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "message" to "Delivery Service is healthy"
        )
    }
}
