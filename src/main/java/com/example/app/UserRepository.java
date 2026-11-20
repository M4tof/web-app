package com.example.app;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Used for account activation:
    Optional<User> findByEmailAndActivationTokenHash(String email, String activationTokenHash);
}