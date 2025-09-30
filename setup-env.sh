#!/bin/bash

# AWS SQS 핸즈온 환경변수 설정 스크립트
# 사용법: source setup-env.sh

echo "🔧 AWS SQS 환경변수 설정 중..."

# AWS 자격증명 (실제 값으로 변경하세요)
export AWS_REGION="ap-northeast-2"
export AWS_ACCESS_KEY_ID="your-access-key-here"
export AWS_SECRET_KEY="your-secret-key-here"

# SQS 대기열 URL (AWS Console에서 생성 후 복사)
# 표준 대기열
export SQS_QUEUE_URL_STANDARD="https://sqs.ap-northeast-2.amazonaws.com/123456789012/order-queue-standard"

# 표준 DLQ
export SQS_QUEUE_URL_STANDARD_DLQ="https://sqs.ap-northeast-2.amazonaws.com/123456789012/order-queue-standard-dlq"

# FIFO 대기열
export SQS_QUEUE_URL_FIFO="https://sqs.ap-northeast-2.amazonaws.com/123456789012/order-queue.fifo"

# FIFO DLQ
export SQS_QUEUE_URL_FIFO_DLQ="https://sqs.ap-northeast-2.amazonaws.com/123456789012/order-queue-dlq.fifo"

echo "✅ 환경변수 설정 완료!"
echo ""
echo "📋 설정된 환경변수:"
echo "   AWS_REGION: $AWS_REGION"
echo "   SQS_QUEUE_URL_STANDARD: $SQS_QUEUE_URL_STANDARD"
echo "   SQS_QUEUE_URL_STANDARD_DLQ: $SQS_QUEUE_URL_STANDARD_DLQ"
echo "   SQS_QUEUE_URL_FIFO: $SQS_QUEUE_URL_FIFO"
echo "   SQS_QUEUE_URL_FIFO_DLQ: $SQS_QUEUE_URL_FIFO_DLQ"
echo ""
echo "💡 이제 서비스를 실행하세요:"
echo "   ./gradlew :order-service:bootRun"
echo "   ./gradlew :delivery-service:bootRun"
