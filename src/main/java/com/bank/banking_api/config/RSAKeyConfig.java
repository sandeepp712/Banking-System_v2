package com.bank.banking_api.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RSAKeyConfig {

    @Value("${JWT_PUBLIC_KEY:MISSING}")
    private String publicKeyStr;

    @Value("${JWT_PRIVATE_KEY:MISSING}")
    public String privateKeyStr;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() {
        // 1. Diagnostic Check: If it hits the fallback, throw an explicitly helpful error
        if (publicKeyStr.equals("MISSING") || privateKeyStr.equals("MISSING")) {
            throw new IllegalStateException(
                    "CRITICAL: Could not read JWT keys from environment variables." +
                            "Verify your .env file exist in the root folder and your IDE/build profile is loading it."
            );
        }

        try {
            // Remove any potential whitespace character buffers
            String cleanPub = publicKeyStr.trim().replaceAll("\\s+", "");
            String cleanPriv = privateKeyStr.trim().replaceAll("\\s+", "");

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            //2 Decode Public Key (X509 Standard)
            byte[] publicBytes = Base64.getDecoder().decode(cleanPub);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicBytes);
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            // 3. Decode Private Key (PKCS8 Standard)
            byte[] privateBytes = Base64.getDecoder().decode(cleanPriv);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateBytes);
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("CRITICAL: JWT Key strings are not valid Base64 encoded data.", e);
        } catch (Exception e) {
            throw new IllegalStateException("CRITICAL: Failed to initialize RSA Key pairs from strings.", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }
}