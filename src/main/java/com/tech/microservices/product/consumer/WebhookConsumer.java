package com.tech.microservices.product.consumer;

import java.util.List;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import com.tech.microservices.product.config.KafkaTopicConfig;
import com.tech.microservices.product.dto.WebhookPayload;
import com.tech.microservices.product.entity.WebhookSubscription;
import com.tech.microservices.product.repository.WebhookSubscriptionRepository;
import com.tech.microservices.product.service.WebhookDispatcher;

@Service
public class WebhookConsumer {

    private final WebhookDispatcher dispatcher;
    private final WebhookSubscriptionRepository subscriptionRepository;

    public WebhookConsumer(WebhookDispatcher dispatcher, WebhookSubscriptionRepository subscriptionRepository) {
        this.dispatcher = dispatcher;
        this.subscriptionRepository = subscriptionRepository;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopicConfig.WEBHOOK_INGEST_TOPIC, groupId = "webhook-dispatch-group")
    public void consume(WebhookPayload payload) {
        System.out.println("Processing event: " + payload.eventId() + " [" + payload.eventType() + "]");
        List<WebhookSubscription> subscriptions = subscriptionRepository.findByEventTypeAndActiveTrue(payload.eventType());

        if (subscriptions.isEmpty()) {
            System.out.println("No active subscribers found for event: " + payload.eventType());
            return;
        }

        for (WebhookSubscription sub : subscriptions) {
            boolean delivered = dispatcher.dispatch(sub.getTargetUrl(), sub.getSecretKey(), payload, 1);
            if (!delivered) {
                throw new RuntimeException("Dispatch failed for subscriber: " + sub.getMerchantId());
            }
        }
    }

    @DltHandler
    public void handleDlt(WebhookPayload payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.err.println("Event permanently failed and moved to DLQ [" + topic + "]: " + payload.eventId());
    }
}