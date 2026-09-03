package com.tech.microservices.product.service;

import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.microservices.product.dto.WebhookPayload;
import com.tech.microservices.product.entity.WebhookDeliveryLog;
import com.tech.microservices.product.repository.WebhookDeliveryLogRepository;
import com.tech.microservices.product.util.HmacSigner;

@Service
public class WebhookDispatcher {
	 private final RestClient restClient;
	    private final HmacSigner hmacSigner;
	    private final ObjectMapper objectMapper;
	    private final IdempotencyService idempotencyService;
	    private final WebhookDeliveryLogRepository logRepository;

	    public WebhookDispatcher(HmacSigner hmacSigner,
	                             ObjectMapper objectMapper,
	                             IdempotencyService idempotencyService,
	                             WebhookDeliveryLogRepository logRepository) {
	        this.restClient = RestClient.builder().build();
	        this.hmacSigner = hmacSigner;
	        this.objectMapper = objectMapper;
	        this.idempotencyService = idempotencyService;
	        this.logRepository = logRepository;
	    }

	    public boolean dispatch(String targetUrl, String secretKey, WebhookPayload payload, int attempt) {
	        if (attempt == 1 && !idempotencyService.lockEvent(payload.eventId())) {
	            System.out.println("Duplicate event detected [" + payload.eventId() + "]. Skipping dispatch.");
	            return false;
	        }

	        long startTime = System.currentTimeMillis();
	        int statusCode = 0;
	        String responseBody = null;
	        boolean success = false;

	        try {
	            String jsonPayload = objectMapper.writeValueAsString(payload);
	            long timestamp = Instant.now().getEpochSecond();
	            String signatureHeader = hmacSigner.generateSignature(jsonPayload, secretKey, timestamp);

	            ResponseEntity<String> response = restClient.post()
	                    .uri(targetUrl)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .header("X-Webhook-Signature", signatureHeader)
	                    .header("X-Webhook-Id", payload.eventId())
	                    .header("X-Webhook-Timestamp", String.valueOf(timestamp))
	                    .body(jsonPayload)
	                    .retrieve()
	                    .toEntity(String.class);

	            statusCode = response.getStatusCode().value();
	            responseBody = response.getBody();
	            success = response.getStatusCode().is2xxSuccessful();

	            if (success) {
	                idempotencyService.markCompleted(payload.eventId());
	            }
	            return success;
	        } catch (Exception ex) {
	            statusCode = 500;
	            responseBody = ex.getMessage();
	            return false;
	        } finally {
	            long executionTime = System.currentTimeMillis() - startTime;
	            
	            WebhookDeliveryLog log = WebhookDeliveryLog.builder()
	                    .eventId(payload.eventId())
	                    .targetUrl(targetUrl)
	                    .responseStatusCode(statusCode)
	                    .responseBody(responseBody != null && responseBody.length() > 1000 ? responseBody.substring(0, 1000) : responseBody)
	                    .executionTimeMs(executionTime)
	                    .successful(success)
	                    .attemptNumber(attempt)
	                    .deliveredAt(Instant.now())
	                    .build();

	            logRepository.save(log);
	        }
	    }
}
