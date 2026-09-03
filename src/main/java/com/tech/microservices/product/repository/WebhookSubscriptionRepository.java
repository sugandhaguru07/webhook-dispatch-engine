package com.tech.microservices.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tech.microservices.product.entity.WebhookSubscription;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription,Long>{
   List<WebhookSubscription> findByEventTypeAndActiveTrue(String eventType);
}
