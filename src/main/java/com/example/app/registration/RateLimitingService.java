package com.example.app.registration;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // 1. Device limit (Specific browser behind the IP)
    private static final int MAX_DEVICE_ATTEMPTS = 5;

    // 2. Global IP threshold (Emergency flood protection for the whole network)
    private static final int MAX_GLOBAL_IP_ATTEMPTS = 25;

    private static final long TIME_WINDOW_SECONDS = 300;   // 5 minutes
    private static final long BLOCK_DURATION_SECONDS = 300; // 5 minutes penalty

    private final Map<String, ClientTracker> trackers = new ConcurrentHashMap<>();

    private static class ClientTracker {
        int attemptCount = 0;
        Instant windowStart = Instant.now();
        Instant blockedUntil = null;
    }

    /**
     * Checks rate limits for both the composite (IP + User-Agent) and the entire IP.
     * @return 0 if allowed; or remaining penalty seconds if blocked.
     */
    public synchronized long checkRateLimit(String ip, String userAgent) {
        Instant now = Instant.now();

        // Safe User-Agent fallback + hash to keep memory keys small
        String uaHash = (userAgent != null) ? Integer.toHexString(userAgent.hashCode()) : "unknown";
        String deviceKey = ip + "#" + uaHash;
        String globalIpKey = "GLOBAL#" + ip;

        // Check 1: Is this specific device/browser over the limit?
        long deviceBlock = checkBucket(deviceKey, MAX_DEVICE_ATTEMPTS, now);
        if (deviceBlock > 0) {
            System.out.println("RATE LIMIT (DEVICE): Blocked " + deviceKey + " for " + deviceBlock + "s");
            return deviceBlock;
        }

        // Check 2: Is the entire IP hammering the server (bot rotating User-Agents)?
        long globalBlock = checkBucket(globalIpKey, MAX_GLOBAL_IP_ATTEMPTS, now);
        if (globalBlock > 0) {
            System.out.println("RATE LIMIT (GLOBAL IP): Flood detected from IP [" + ip + "]. Blocked for " + globalBlock + "s");
            return globalBlock;
        }

        return 0; // Allowed
    }

    private long checkBucket(String key, int maxAttempts, Instant now) {
        ClientTracker tracker = trackers.computeIfAbsent(key, k -> new ClientTracker());

        // Check active block
        if (tracker.blockedUntil != null) {
            if (now.isBefore(tracker.blockedUntil)) {
                return tracker.blockedUntil.getEpochSecond() - now.getEpochSecond();
            } else {
                tracker.blockedUntil = null;
                tracker.attemptCount = 0;
                tracker.windowStart = now;
            }
        }

        // Reset sliding window
        if (now.isAfter(tracker.windowStart.plusSeconds(TIME_WINDOW_SECONDS))) {
            tracker.attemptCount = 0;
            tracker.windowStart = now;
        }

        tracker.attemptCount++;

        if (tracker.attemptCount > maxAttempts) {
            tracker.blockedUntil = now.plusSeconds(BLOCK_DURATION_SECONDS);
            return BLOCK_DURATION_SECONDS;
        }

        return 0;
    }

    public static String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}