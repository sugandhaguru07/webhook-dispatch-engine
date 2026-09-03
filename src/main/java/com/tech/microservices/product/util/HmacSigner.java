package com.tech.microservices.product.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class HmacSigner {
	private static final String HMAC_SHA256 = "HmacSHA256";

    public String generateSignature(String payload, String secret, long timestamp) {
        try {
            String signaturePayload = timestamp + "." + payload;
            
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(signaturePayload.getBytes(StandardCharsets.UTF_8));
            String hexSignature = HexFormat.of().formatHex(rawHmac);

            return "t=" + timestamp + ",v1=" + hexSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }
}
