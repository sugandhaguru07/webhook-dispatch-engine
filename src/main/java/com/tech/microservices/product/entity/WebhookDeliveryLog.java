package com.tech.microservices.product.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_delivery_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryLog {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(nullable = false)
	    private String eventId;

	    @Column(nullable = false)
	    private String targetUrl;

	    private int responseStatusCode;

	    @Column(columnDefinition = "TEXT")
	    private String responseBody;

	    private long executionTimeMs;

	    @Column(nullable = false)
	    private boolean successful;

	    private int attemptNumber;

	    @Column(nullable = false)
	    private Instant deliveredAt;
}
