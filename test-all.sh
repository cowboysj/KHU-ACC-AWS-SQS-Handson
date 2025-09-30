#!/bin/bash

# AWS SQS 핸즈온 테스트 스크립트

echo "🧪 AWS SQS 핸즈온 테스트 시작"
echo "================================"
echo ""

BASE_URL="http://localhost:8080"

echo "1️⃣ 표준 대기열 테스트"
echo "-------------------"

# 정상 주문 (표준)
echo "📦 정상 주문 생성 (표준 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=standard" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "items": [
      {"productId": "p1", "productName": "노트북", "quantity": 1, "price": 1500000},
      {"productId": "p2", "productName": "마우스", "quantity": 2, "price": 30000}
    ]
  }' | jq '.'

sleep 2

echo ""
echo "📦 두 번째 주문 (표준 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=standard" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-002",
    "items": [
      {"productId": "p3", "productName": "키보드", "quantity": 1, "price": 150000}
    ]
  }' | jq '.'

sleep 2

echo ""
echo "📦 세 번째 주문 (표준 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=standard" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-003",
    "items": [
      {"productId": "p4", "productName": "모니터", "quantity": 1, "price": 500000}
    ]
  }' | jq '.'

echo ""
echo "⏸️  5초 대기 중... (메시지 처리 확인)"
sleep 5

echo ""
echo "================================"
echo ""
echo "2️⃣ DLQ 테스트 (표준 대기열)"
echo "-------------------"

# 실패하는 주문
echo "❌ 실패하는 주문 생성 (DLQ 테스트)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=standard" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-004",
    "items": [
      {"productId": "p999", "productName": "실패테스트", "quantity": 1, "price": 10000}
    ]
  }' | jq '.'

echo ""
echo "⏸️  90초 대기 중... (3번 재시도 후 DLQ 이동 확인)"
echo "   → Delivery Service 로그에서 3번 시도 확인"
sleep 90

echo ""
echo "================================"
echo ""
echo "3️⃣ FIFO 대기열 테스트"
echo "-------------------"

# 같은 고객의 순차적 주문 (순서 보장 테스트)
echo "📦 첫 번째 주문 (FIFO 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-fifo-001",
    "items": [
      {"productId": "f1", "productName": "FIFO-첫번째", "quantity": 1, "price": 10000}
    ]
  }' | jq '.'

sleep 1

echo ""
echo "📦 두 번째 주문 (FIFO 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-fifo-001",
    "items": [
      {"productId": "f2", "productName": "FIFO-두번째", "quantity": 1, "price": 20000}
    ]
  }' | jq '.'

sleep 1

echo ""
echo "📦 세 번째 주문 (FIFO 대기열)..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-fifo-001",
    "items": [
      {"productId": "f3", "productName": "FIFO-세번째", "quantity": 1, "price": 30000}
    ]
  }' | jq '.'

echo ""
echo "⏸️  5초 대기 중... (순서 보장 확인)"
sleep 5

echo ""
echo "================================"
echo ""
echo "4️⃣ 다른 고객의 FIFO 주문 (병렬 처리 테스트)"
echo "-------------------"

# 다른 고객 (다른 메시지 그룹)
echo "📦 고객 A의 주문..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-A",
    "items": [
      {"productId": "fa1", "productName": "고객A-상품1", "quantity": 1, "price": 10000}
    ]
  }' | jq '.'

echo ""
echo "📦 고객 B의 주문..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-B",
    "items": [
      {"productId": "fb1", "productName": "고객B-상품1", "quantity": 1, "price": 20000}
    ]
  }' | jq '.'

echo ""
echo "📦 고객 A의 두 번째 주문..."
curl -s -X POST "$BASE_URL/api/orders?queueType=fifo" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-A",
    "items": [
      {"productId": "fa2", "productName": "고객A-상품2", "quantity": 1, "price": 15000}
    ]
  }' | jq '.'

echo ""
echo "⏸️  5초 대기 중..."
sleep 5

echo ""
echo "================================"
echo "✅ 테스트 완료!"
echo ""
echo "📊 결과 확인:"
echo "1. 표준 대기열: 순서가 보장되지 않을 수 있음"
echo "2. DLQ: '실패테스트' 주문이 3번 시도 후 DLQ로 이동"
echo "3. FIFO 대기열: 같은 고객 내에서 순서 보장"
echo "4. 다른 고객: 병렬로 처리되지만 각 고객 내에서는 순서 보장"
echo ""
echo "💡 Delivery Service 로그를 확인하세요!"
