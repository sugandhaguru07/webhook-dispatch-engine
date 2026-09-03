# Webhook Dispatch & Delivery Engine

A fault-tolerant, high-throughput webhook delivery engine built with **Spring Boot 3**, **Java 21**, and **Apache Kafka**. Decouples event ingestion from external network I/O, guarantees idempotent event processing via **Redis**, secures payloads using **HMAC-SHA256**, and provides reliability through non-blocking exponential backoff retries and a **Dead Letter Queue (DLQ)**.

---

## Architecture Overview

```text
[Client Ingest] 
      │ (HTTP POST - Returns 202 Accepted < 15ms)
      ▼
[WebhookIngestController] ──► [Kafka Topic: webhook-ingest]
                                       │
                                       ▼
                              [WebhookConsumer]
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            ▼                          ▼                          ▼
    [Redis Idempotency]       [Postgres Subscriptions]    [HMAC-SHA256 Signer]
   (SETNX 24hr atomic lock)    (Active subscriber lookup)  (t=timestamp, v1=hash)
            │                          │                          │
            └──────────────────────────┼──────────────────────────┘
                                       ▼
                            [WebhookDispatcher]
                                       │ (HTTP POST with Signatures)
                                       ▼
                           [Target Merchant Server]
                                       │
               ┌───────────────────────┴───────────────────────┐
         (HTTP 2xx)                                      (HTTP Non-2xx / Timeout)
               │                                               │
               ▼                                               ▼
     [PostgreSQL Audit Log]                       [@RetryableTopic Backoff]
     (Latency & status stored)                    (Delay: 1s, 2s)
                                                               │
                                                       (Retries Exhausted)
                                                               │
                                                               ▼
                                                    [DLQ: webhook-ingest-dlt]
                                                    (@DltHandler isolation)

Key Engineering Highlights
Asynchronous Decoupled Ingestion: Offloads slow network operations from the client request-response lifecycle. API calls return 202 Accepted in sub-15ms while dispatch runs in worker consumers.

Distributed Idempotency (Redis SETNX): Protects against Kafka's at-least-once duplicate delivery. Uses atomic Redis key locks with a 24-hour TTL to prevent double-charging or duplicate event handling.

Cryptographic Payload Security (HMAC-SHA256): Prevents payload tampering and replay attacks. Calculates SHA256 hashes using a shared secret and adds X-Webhook-Signature (t=timestamp,v1=hash).

Non-Blocking Resiliency Pipeline: Uses Spring Kafka @RetryableTopic for multi-tier exponential backoff without blocking consumer threads. Permanently failed events route to a Dead Letter Topic (webhook-ingest-dlt).

Auditing & Observability: Automatically persists latency timings, raw HTTP status codes, and outbound payloads to PostgreSQL for merchant troubleshooting.

Tech Stack
Language & Framework: Java 21, Spring Boot 3.3.4

Messaging Broker: Apache Kafka 3.7 (KRaft mode)

In-Memory Cache: Redis 7 (Idempotency locking)

Relational Database: PostgreSQL 16 (Subscriptions & Delivery Audit Trails)

Containerization: Docker & Docker Compose

HTTP Client: Spring RestClient

Prerequisites:
Java 21 SDK
Docker and Docker Compose
Maven Wrapper
Postman (for API execution and inspection)

1. Start Infrastructure
Launch the backing services (Kafka, Redis, PostgreSQL): docker compose up -d
Verify the containers are healthy: docker ps
2. Run the Application
./mvnw clean spring-boot:run
The server runs on port 8080.

Step-by-Step API Execution Workflow
Step 1: Register a Subscriber
Request: POST http://localhost:8080/api/v1/management/subscriptions

Body:
{
  "merchantId": "merchant_amazon",
  "targetUrl": "[https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0](https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0)",
  "eventType": "payment.succeeded"
}

Response (200 OK):
{
  "id": 1,
  "merchantId": "merchant_amazon",
  "targetUrl": "[https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0](https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0)",
  "secretKey": "whsec_70209bd2f423e1cd93e1668e6251c20d15c2a267be28f1d4",
  "eventType": "payment.succeeded",
  "active": true,
  "createdAt": "2026-08-27T20:12:53.325Z"
}

Step 2: Ingest an Event
Request: POST http://localhost:8080/api/v1/webhooks/send

Body:
{
  "orderId": "ORD-5050",
  "amount": 4999,
  "currency": "INR",
  "status": "SUCCESS"
}

Response (202 Accepted):
{
  "status": "QUEUED",
  "eventId": "f93d3d95-d75a-4ac1-b1c4-e9e84d5819cf"
}

Step 3: Query Delivery Audit Log
Request: GET http://localhost:8080/api/v1/management/logs/f93d3d95-d75a-4ac1-b1c4-e9e84d5819cf
Body: none

Response (200 OK):
[
  {
    "id": 4,
    "eventId": "f93d3d95-d75a-4ac1-b1c4-e9e84d5819cf",
    "targetUrl": "[https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0](https://webhook.site/b58a442b-cad8-474c-959d-ab369182c2f0)",
    "responseStatusCode": 200,
    "executionTimeMs": 2409,
    "successful": true,
    "attemptNumber": 1,
    "deliveredAt": "2026-08-27T20:21:06.956Z"
  }
]
