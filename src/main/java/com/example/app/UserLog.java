package com.example.app;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_logs", schema = "app_auth")
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "failed_attempts", nullable = false)
    private long failedAttempts = 1; // Default to 1 on failure

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UserLog() {}

    public UserLog(Long userId, String eventType, String ipAddress, String userAgent, long failedAttempts) {
        this.userId = userId;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.failedAttempts = failedAttempts;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEventType() { return eventType; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }

    public long getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(long failedAttempts) { this.failedAttempts = failedAttempts; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}