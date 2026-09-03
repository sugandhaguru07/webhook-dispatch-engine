package com.tech.microservices.product.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tech.microservices.product.config.KafkaTopicConfig;
import com.tech.microservices.product.dto.WebhookPayload;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookIngestController {

    private final KafkaTemplate<String, WebhookPayload> kafkaTemplate;

    public WebhookIngestController(KafkaTemplate<String, WebhookPayload> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> enqueueWebhook(@RequestBody Map<String, Object> data) {
        String eventId = UUID.randomUUID().toString();
        
        WebhookPayload payload = new WebhookPayload(
                eventId,
                "payment.succeeded",
                Instant.now(),
                data
        );

        kafkaTemplate.send(KafkaTopicConfig.WEBHOOK_INGEST_TOPIC, eventId, payload);

        return ResponseEntity.accepted().body(Map.of(
                "status", "QUEUED",
                "eventId", eventId
        ));
    }
}
