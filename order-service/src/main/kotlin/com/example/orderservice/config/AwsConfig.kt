package com.example.orderservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class AwsConfig {

    @Value("\${aws.sqs.endpoint:}")
    private lateinit var sqsEndpoint: String

    @Value("\${aws.region:ap-northeast-2}")
    private lateinit var region: String

    @Value("\${aws.accessKeyId:test}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretKey:test}")
    private lateinit var secretKey: String

    @Bean
    fun sqsClient(): SqsClient {
        val builder = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretKey)
                )
            )

        if (sqsEndpoint.isNotEmpty()) {
            builder.endpointOverride(URI.create(sqsEndpoint))
        }

        return builder.build()
    }
}
