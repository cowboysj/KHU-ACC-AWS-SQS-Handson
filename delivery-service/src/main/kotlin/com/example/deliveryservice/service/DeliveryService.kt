package com.example.deliveryservice.service

import com.example.core.event.OrderEvent
import com.example.core.event.DeliveryEvent
import com.example.core.event.DeliveryStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class DeliveryService {
    private val logger = LoggerFactory.getLogger(DeliveryService::class.java)

    fun processOrder(orderEvent: OrderEvent, queueType: String = "STANDARD") {
        logger.info("🚚 [$queueType] 배송 처리 시작 - OrderId: ${orderEvent.orderId}")
        
        // 배송 정보 생성
        val deliveryEvent = DeliveryEvent(
            deliveryId = UUID.randomUUID().toString(),
            orderId = orderEvent.orderId,
            address = "서울시 강남구 테헤란로 123",
            status = DeliveryStatus.PENDING
        )
        
        logger.info("📋 [$queueType] 배송 정보:")
        logger.info("   ├─ Delivery ID: ${deliveryEvent.deliveryId}")
        logger.info("   ├─ Order ID: ${orderEvent.orderId}")
        logger.info("   ├─ Customer: ${orderEvent.customerId}")
        logger.info("   ├─ Items: ${orderEvent.items.map { "${it.productName}(${it.quantity}개)" }}")
        logger.info("   └─ Total: ${orderEvent.totalAmount}원")
        
        // 배송 처리 시뮬레이션
        Thread.sleep(500)
        
        logger.info("✅ [$queueType] 배송 처리 완료 - DeliveryId: ${deliveryEvent.deliveryId}, OrderId: ${orderEvent.orderId}")
    }
}
