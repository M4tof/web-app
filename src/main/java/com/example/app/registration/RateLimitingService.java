package com.example.app.registration;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // Configurable limits
    private static final int MAX_ATTEMPTS = 5;
    private static final long TIME_WINDOW_SECONDS = 300; // 5 minutes
    private static final long BLOCK_DURATION_SECONDS = 300; // 5 minutes penalty

    // IP -> Attempt Tracker
    private final Map<String, ClientTracker> ipTrackers = new ConcurrentHashMap<>();

    private static class ClientTracker {
        int attemptCount = 0;
        Instant windowStart = Instant.now();
        Instant blockedUntil = null;
    }

    /**
     * Checks if the IP is allowed to make a request.
     * @return 0 if allowed; or the number of seconds remaining in the block penalty.
     */
    public synchronized long checkRateLimit(String ip) {
        Instant now = Instant.now();
        ClientTracker tracker = ipTrackers.computeIfAbsent(ip, k -> new ClientTracker());

        // 1. Check if currently blocked
        if (tracker.blockedUntil != null) {
            if (now.isBefore(tracker.blockedUntil)) {
                // Still blocked -> return remaining seconds
                return tracker.blockedUntil.getEpochSecond() - now.getEpochSecond();
            } else {
                // Block expired -> reset tracker
                tracker.blockedUntil = null;
                tracker.attemptCount = 0;
                tracker.windowStart = now;
            }
        }

        // 2. Check if the sliding time window has expired
        if (now.isAfter(tracker.windowStart.plusSeconds(TIME_WINDOW_SECONDS))) {
            tracker.attemptCount = 0;
            tracker.windowStart = now;
        }

        // 3. Increment attempts
        tracker.attemptCount++;

        // 4. If limit exceeded, trigger the penalty block
        if (tracker.attemptCount > MAX_ATTEMPTS) {
            tracker.blockedUntil = now.plusSeconds(BLOCK_DURATION_SECONDS);
            System.out.println("RATE LIMIT TRIGGERED: IP [" + ip + "] is blocked for 5 minutes.");
            return BLOCK_DURATION_SECONDS;
        }

        return 0; // Allowed
    }

    public static String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        // Take the first IP if there is a comma-separated list of proxies
        return xfHeader.split(",")[0].trim();
    }
}