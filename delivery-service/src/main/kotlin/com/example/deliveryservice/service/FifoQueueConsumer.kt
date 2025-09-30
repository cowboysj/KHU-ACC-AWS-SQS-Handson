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
class FifoQueueConsumer(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    private val deliveryService: DeliveryService,
    @Value("\${aws.sqs.queue-url-fifo}") private val queueUrl: String
) {
    private val logger = LoggerFactory.getLogger(FifoQueueConsumer::class.java)
    private val executorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var isRunning = false

    @PostConstruct
    fun startConsuming() {
        if (queueUrl.isEmpty()) {
            logger.warn("⚠️ [FIFO] Queue URL이 설정되지 않아 Consumer를 시작하지 않습니다.")
            return
        }
        
        isRunning = true
        executorService.submit {
            logger.info("👂 [FIFO] SQS 메시지 폴링 시작... Queue: $queueUrl")
            
            while (isRunning) {
                try {
                    pollMessages()
                } catch (e: Exception) {
                    logger.error("❌ [FIFO] 메시지 폴링 중 오류 발생", e)
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

        logger.info("📬 [FIFO] ${messages.size}개의 메시지 수신")

        messages.forEach { message ->
            try {
                logger.info("📨 [FIFO] 메시지 처리 시작 - MessageId: ${message.messageId()}")
                
                val orderEvent = objectMapper.readValue(message.body(), OrderEvent::class.java)
                
                deliveryService.processOrder(orderEvent, "FIFO")
                deleteMessage(message.receiptHandle())
                
                logger.info("✅ [FIFO] 메시지 처리 완료 - MessageId: ${message.messageId()}, 순서 보장됨")
            } catch (e: Exception) {
                logger.error("❌ [FIFO] 메시지 처리 실패 - MessageId: ${message.messageId()}", e)
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
        logger.info("🛑 [FIFO] SQS 메시지 폴링 중지 중...")
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
