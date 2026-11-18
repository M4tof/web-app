package com.example.app;

import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    private final UserRepository userRepository;

    public ApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Matches the JSON payload sent by the frontend
    public record RegisterRequest(String email, String password, String nickname) {}

    @PostMapping("/api/register")
    public String handleRegistration(@RequestBody RegisterRequest request) {
//        TODO!!!!

        // 1. Honeypot check: If the hidden nickname field has text, a bot filled it
        if (request.nickname() != null && !request.nickname().isBlank()) {
            System.out.println("ALERT: Honeypot triggered! Rejected bot submission.");
            return "Registration error.";
        }

        // 2. Create the user entity (storing plain text for now, hashing comes later)
        User newUser = new User(request.email(), request.password());

        // 3. Save into the database
        User savedUser = userRepository.save(newUser);

        System.out.println("LOG: Successfully registered user #" + savedUser.getId() + " (" + savedUser.getEmail() + ")");

        return "User registered successfully! Assigned DB ID: " + savedUser.getId();
    }
}