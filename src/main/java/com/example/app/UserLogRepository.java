package com.example.app;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    // Finds the latest log entry for this IP and event type
    Optional<UserLog> findFirstByIpAddressAndEventTypeOrderByCreatedAtDesc(String ipAddress, String eventType);
}