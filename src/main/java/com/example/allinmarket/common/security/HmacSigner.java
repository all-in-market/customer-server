package com.example.allinmarket.common.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    public static String sign(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            mac.init(keySpec);

            byte[] hmac = mac.doFinal(
                    message.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hmac);

        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }
}
