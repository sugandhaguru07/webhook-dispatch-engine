package com.tech.microservices.product.dto;

import java.time.Instant;
import java.util.Map;

public record WebhookPayload(
	    String eventId,
	    String eventType,
	    Instant timestamp,
	    Map<String, Object> data
	) {}