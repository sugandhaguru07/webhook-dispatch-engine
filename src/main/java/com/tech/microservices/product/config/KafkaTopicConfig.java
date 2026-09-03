package com.tech.microservices.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
	 public static final String WEBHOOK_INGEST_TOPIC = "webhook-ingest";

	    @Bean
	    public NewTopic webhookIngestTopic() {
	        return TopicBuilder.name(WEBHOOK_INGEST_TOPIC)
	                .partitions(3)
	                .replicas(1)
	                .build();
	    }
}
