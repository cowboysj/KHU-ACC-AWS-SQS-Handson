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
class DlqMonitorService(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queue-url-standard-dlq}") private val standardDlqUrl: String,
    @Value("\${aws.sqs.queue-url-fifo-dlq}") private val fifoDlqUrl: String
) {
    private val logger = LoggerFactory.getLogger(DlqMonitorService::class.java)
    private val executorService = Executors.newFixedThreadPool(2)
    @Volatile
    private var isRunning = false

    @PostConstruct
    fun startMonitoring() {
        isRunning = true
        
        // Standard DLQ 모니터링
        if (standardDlqUrl.isNotEmpty()) {
            executorService.submit {
                logger.info("👀 [STANDARD DLQ] 모니터링 시작...")
                while (isRunning) {
                    try {
                        monitorQueue(standardDlqUrl, "STANDARD")
                    } catch (e: Exception) {
                        logger.error("❌ [STANDARD DLQ] 모니터링 중 오류", e)
                    }
                    Thread.sleep(30000) // 30초마다 체크
                }
            }
        }
        
        // FIFO DLQ 모니터링
        if (fifoDlqUrl.isNotEmpty()) {
            executorService.submit {
                logger.info("👀 [FIFO DLQ] 모니터링 시작...")
                while (isRunning) {
                    try {
                        monitorQueue(fifoDlqUrl, "FIFO")
                    } catch (e: Exception) {
                        logger.error("❌ [FIFO DLQ] 모니터링 중 오류", e)
                    }
                    Thread.sleep(30000) // 30초마다 체크
                }
            }
        }
    }

    private fun monitorQueue(queueUrl: String, queueType: String) {
        val receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(5)
            .build()

        val messages = sqsClient.receiveMessage(receiveRequest).messages()

        if (messages.isNotEmpty()) {
            logger.error("🚨 [$queueType DLQ] ${messages.size}개의 실패 메시지 감지!")
            
            messages.forEach { message ->
                try {
                    val orderEvent = objectMapper.readValue(message.body(), OrderEvent::class.java)
                    
                    logger.error("   ├─ Order ID: ${orderEvent.orderId}")
                    logger.error("   ├─ Customer ID: ${orderEvent.customerId}")
                    logger.error("   ├─ Items: ${orderEvent.items.map { it.productName }}")
                    logger.error("   └─ Timestamp: ${orderEvent.timestamp}")
                    
                    // DLQ에서는 메시지를 삭제하지 않고 유지 (수동 처리를 위해)
                    // 필요시 삭제: deleteMessage(queueUrl, message.receiptHandle())
                    
                } catch (e: Exception) {
                    logger.error("❌ [$queueType DLQ] 메시지 파싱 실패", e)
                }
            }
            
            logger.error("💡 [$queueType DLQ] 수동 처리 또는 재처리가 필요합니다.")
        }
    }

    private fun deleteMessage(queueUrl: String, receiptHandle: String) {
        val deleteRequest = DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle)
            .build()
        
        sqsClient.deleteMessage(deleteRequest)
    }

    @PreDestroy
    fun stopMonitoring() {
        logger.info("🛑 DLQ 모니터링 중지 중...")
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
