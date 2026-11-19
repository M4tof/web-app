package com.example.app;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Entity
@Table(name = "users", schema = "app_auth")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_activated", nullable = false)
    private boolean isActivated = false;

    @Column(name = "activation_token_hash")
    private String activationTokenHash;

    @Column(name = "activation_expires_at")
    private Instant activationExpiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // Default constructor required by JPA
    public User() {}

    public User(String email, String passwordHash, String activationTokenHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.activationTokenHash = activationTokenHash;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActivated() { return isActivated; }
    public void setActivated(boolean activated) { isActivated = activated; }
    public String getActivationTokenHash() { return activationTokenHash; }
    public void setActivationTokenHash(String activationTokenHash) { this.activationTokenHash = activationTokenHash; }
    public Instant getActivationExpiresAt() { return activationExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}