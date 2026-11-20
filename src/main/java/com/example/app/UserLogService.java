package com.example.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UserLogService {

    private final UserLogRepository userLogRepository;

    public UserLogService(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String eventType, String ipAddress, String userAgent) {
        try {
            // 1. Successful events are always recorded as distinct rows
            if ("REGISTRATION_SUCCESS".equals(eventType) || "ACTIVATION_SUCCESS".equals(eventType)) {
                UserLog log = new UserLog(userId, eventType, ipAddress, userAgent, 0);
                userLogRepository.save(log);
                return;
            }

            // 2. For failures (e.g. DUPLICATE_REG_ATTEMPT): check if we logged this IP recently (last 1 hour)
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            Optional<UserLog> recentLogOpt = userLogRepository
                    .findFirstByIpAddressAndEventTypeOrderByCreatedAtDesc(ipAddress, eventType);

            if (recentLogOpt.isPresent() && recentLogOpt.get().getCreatedAt().isAfter(oneHourAgo)) {
                UserLog existingLog = recentLogOpt.get();
                existingLog.setFailedAttempts(existingLog.getFailedAttempts() + 1);
                existingLog.setCreatedAt(Instant.now());
                userLogRepository.save(existingLog);
            } else {
                UserLog newLog = new UserLog(userId, eventType, ipAddress, userAgent, 1);
                userLogRepository.save(newLog);
            }

        } catch (Exception e) {
            System.err.println("FAILED TO WRITE AUDIT LOG: " + e.getMessage());
        }
    }
}