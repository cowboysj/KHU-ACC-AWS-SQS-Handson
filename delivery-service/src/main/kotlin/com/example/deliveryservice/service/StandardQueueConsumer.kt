package com.example.deliveryservice.service

import com.example.core.event.OrderEvent
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class StandardQueueConsumer(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    private val deliveryService: DeliveryService,
    @Value("\${aws.sqs.queue-url-standard}") private val queueUrl: String
) {
    private val logger = LoggerFactory.getLogger(StandardQueueConsumer::class.java)
    private val executorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var isRunning = false
    
    // DLQ 테스트를 위한 실패 시뮬레이터
    private val failureSimulator = mutableMapOf<String, Int>()

    @PostConstruct
    fun startConsuming() {
        if (queueUrl.isEmpty()) {
            logger.warn("⚠️ [STANDARD] Queue URL이 설정되지 않아 Consumer를 시작하지 않습니다.")
            return
        }
        
        isRunning = true
        executorService.submit {
            logger.info("👂 [STANDARD] SQS 메시지 폴링 시작... Queue: $queueUrl")
            
            while (isRunning) {
                try {
                    pollMessages()
                } catch (e: Exception) {
                    logger.error("❌ [STANDARD] 메시지 폴링 중 오류 발생", e)
                    Thread.sleep(5000)
                }
            }
        }
    }

    private fun pollMessages() {
        val receiveMessageRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(20)
            .build()

        val messages = sqsClient.receiveMessage(receiveMessageRequest).messages()

        if (messages.isEmpty()) {
            return
        }

        logger.info("📬 [STANDARD] ${messages.size}개의 메시지 수신")

        messages.forEach { message ->
            try {
                logger.info("📨 [STANDARD] 메시지 처리 시작 - MessageId: ${message.messageId()}")
                
                val orderEvent = objectMapper.readValue(message.body(), OrderEvent::class.java)
                
                // ⚠️ DLQ 테스트: "실패테스트" 상품은 의도적으로 실패시킴
                if (orderEvent.items.any { it.productName.contains("실패테스트", ignoreCase = true) }) {
                    val attemptCount = failureSimulator.getOrDefault(orderEvent.orderId, 0) + 1
                    failureSimulator[orderEvent.orderId] = attemptCount
                    
                    logger.error("❌ [STANDARD] 처리 실패 시뮬레이션 - Order ID: {}, 시도 횟수: {}", 
                        orderEvent.orderId, attemptCount)
                    
                    throw RuntimeException("의도적인 처리 실패 (DLQ 테스트용)")
                }
                
                deliveryService.processOrder(orderEvent, "STANDARD")
                deleteMessage(message.receiptHandle())
                
                logger.info("✅ [STANDARD] 메시지 처리 완료 - MessageId: ${message.messageId()}")
            } catch (e: Exception) {
                logger.error("❌ [STANDARD] 메시지 처리 실패 - MessageId: ${message.messageId()}", e)
            }
        }
    }

    private fun deleteMessage(receiptHandle: String) {
        val deleteRequest = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle)
            .build()
        
        sqsClient.deleteMessage(deleteRequest)
    }

    @PreDestroy
    fun stopConsuming() {
        logger.info("🛑 [STANDARD] SQS 메시지 폴링 중지 중...")
        isRunning = false
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}
