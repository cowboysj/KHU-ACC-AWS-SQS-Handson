package com.example.orderservice.service

import com.example.core.event.OrderEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Service
class SqsPublisher(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queue-url-standard}") private val standardQueueUrl: String,
    @Value("\${aws.sqs.queue-url-fifo}") private val fifoQueueUrl: String
) {
    private val logger = LoggerFactory.getLogger(SqsPublisher::class.java)

    fun publishOrderEvent(orderEvent: OrderEvent, queueType: String = "standard") {
        val queueUrl = when (queueType.lowercase()) {
            "fifo" -> fifoQueueUrl
            else -> standardQueueUrl
        }
        
        if (queueUrl.isEmpty()) {
            logger.error("❌ Queue URL이 설정되지 않았습니다 - QueueType: $queueType")
            throw IllegalStateException("Queue URL not configured for type: $queueType")
        }
        
        try {
            val messageBody = objectMapper.writeValueAsString(orderEvent)
            
            val requestBuilder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
            
            // FIFO 대기열인 경우 추가 설정
            if (queueUrl.endsWith(".fifo")) {
                requestBuilder
                    .messageGroupId(orderEvent.customerId) // 고객별로 순서 보장
                    .messageDeduplicationId(orderEvent.orderId) // 중복 제거
                
                logger.info("✅ [FIFO] 주문 메시지 발행 - OrderId: ${orderEvent.orderId}, Group: ${orderEvent.customerId}")
            } else {
                logger.info("✅ [STANDARD] 주문 메시지 발행 - OrderId: ${orderEvent.orderId}")
            }

            val response = sqsClient.sendMessage(requestBuilder.build())
            logger.info("   MessageId: ${response.messageId()}")
        } catch (e: Exception) {
            logger.error("❌ 주문 메시지 발행 실패 - OrderId: ${orderEvent.orderId}, QueueType: $queueType", e)
            throw e
        }
    }
}
