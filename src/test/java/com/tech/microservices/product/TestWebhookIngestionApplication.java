package com.tech.microservices.product;

import org.springframework.boot.SpringApplication;

public class TestWebhookIngestionApplication {

	public static void main(String[] args) {
		SpringApplication.from(WebhookIngestionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
