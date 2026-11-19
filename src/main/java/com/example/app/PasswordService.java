package com.example.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordService {

    private final String pepper;
    private final Argon2PasswordEncoder encoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordService(@Value("${app.security.pepper}") String pepper) {
        this.pepper = pepper;
        // Salt: 16 bytes, Hash: 32 bytes, Parallelism: 1 lane, Memory: 64MB (65536 KB), Iterations: 3
        this.encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    /**
     * Combines the raw password with the server pepper and computes the Argon2id hash.
     */
    public String hashPassword(String rawPassword) {
        String peppered = rawPassword + pepper;
        return encoder.encode(peppered);
    }

    /**
     * Verifies a raw password against the stored Argon2id hash (for logging in later).
     */
    public boolean verifyPassword(String rawPassword, String storedHash) {
        String peppered = rawPassword + pepper;
        return encoder.matches(peppered, storedHash);
    }

    /**
     * Generates a 32-byte (256-bit) cryptographically secure URL-safe token.
     * This raw token is what you send to the user (via email or response).
     */
    public String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes the raw token using SHA-256.
     * This is what gets saved into activation_token_hash in the database.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

}