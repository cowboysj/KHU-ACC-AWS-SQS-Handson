package com.example.orderservice.service

import com.example.core.event.OrderEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val sqsPublisher: SqsPublisher
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    fun createOrder(orderEvent: OrderEvent, queueType: String = "standard"): OrderEvent {
        logger.info("📦 새로운 주문 생성 - OrderId: ${orderEvent.orderId}, QueueType: $queueType")
        
        // SQS로 주문 이벤트 발행
        sqsPublisher.publishOrderEvent(orderEvent, queueType)
        
        logger.info("✅ 주문 처리 완료 - OrderId: ${orderEvent.orderId}")
        return orderEvent
    }
}
