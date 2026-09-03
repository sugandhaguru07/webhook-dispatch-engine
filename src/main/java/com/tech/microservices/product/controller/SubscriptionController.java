package com.tech.microservices.product.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tech.microservices.product.entity.WebhookSubscription;
import com.tech.microservices.product.repository.WebhookSubscriptionRepository;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final WebhookSubscriptionRepository subscriptionRepository;

    public SubscriptionController(WebhookSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostMapping("/register")
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

        WebhookSubscription saved = subscriptionRepository.save(subscription);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<WebhookSubscription>> listSubscriptions() {
        return ResponseEntity.ok(subscriptionRepository.findAll());
    }
}
