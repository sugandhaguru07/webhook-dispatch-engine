package com.tech.microservices.product.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tech.microservices.product.entity.WebhookDeliveryLog;
import com.tech.microservices.product.entity.WebhookSubscription;
import com.tech.microservices.product.repository.WebhookDeliveryLogRepository;
import com.tech.microservices.product.repository.WebhookSubscriptionRepository;

@RestController
@RequestMapping("/api/v1/management")
public class WebhookManagementController {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryLogRepository logRepository;

    public WebhookManagementController(WebhookSubscriptionRepository subscriptionRepository,
                                       WebhookDeliveryLogRepository logRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.logRepository = logRepository;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<WebhookSubscription> registerSubscription(@RequestBody Map<String, String> request) {
        byte[] secretBytes = new byte[24];
        new SecureRandom().nextBytes(secretBytes);
        String generatedSecret = "whsec_" + HexFormat.of().formatHex(secretBytes);

        WebhookSubscription subscription = WebhookSubscription.builder()
                .merchantId(request.get("merchantId"))
                .targetUrl(request.get("targetUrl"))
                .eventType(request.getOrDefault("eventType", "payment.succeeded"))
                .secretKey(generatedSecret)
                .active(true)
                .createdAt(Instant.now())
                .build();

        return ResponseEntity.ok(subscriptionRepository.save(subscription));
    }

    @GetMapping("/logs/{eventId}")
    public ResponseEntity<List<WebhookDeliveryLog>> getLogsByEventId(@PathVariable String eventId) {
        List<WebhookDeliveryLog> logs = logRepository.findByEventId(eventId);
        return ResponseEntity.ok(logs);
    }
}