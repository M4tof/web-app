package com.example.app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final PasswordService passwordService;

    public ApiController(UserRepository userRepository,
                         CaptchaService captchaService,
                         PasswordService passwordService) {
        this.userRepository = userRepository;
        this.captchaService = captchaService;
        this.passwordService = passwordService;
    }

    @GetMapping("/api/captcha")
    public CaptchaService.CaptchaResponse getCaptcha() {
        return captchaService.generateCaptcha();
    }

    // Matches the JSON payload sent by the frontend
    public record RegisterRequest(
            String email,
            String password,
            String nickname,
            String captchaId,
            String captchaAnswer
    ) {}

    @PostMapping("/api/register")
    public ResponseEntity<String> handleRegistration(@RequestBody RegisterRequest request) {

        // Honeypot check
        if (request.nickname() != null && !request.nickname().isBlank()) {
            System.out.println("HONEYPOT TRIGGERED: Bot submitted nickname: \"" + request.nickname() + "\". Dropping silently.");
            // Return fake 200 OK so the bot believes it succeeded
            return ResponseEntity.ok("User registered successfully! Please check your email for activation.");
        }

        // Server-side Captcha validation
        boolean isCaptchaValid = captchaService.validateCaptcha(request.captchaId(), request.captchaAnswer());
        if (!isCaptchaValid) {
            return ResponseEntity.badRequest().body("Captcha verification failed. Please try again.");
        }

        // ARGON2ID HASHING WITH PEPPER
        String hashedPassword = passwordService.hashPassword(request.password());

        // Act token & Hash
        String rawActivationToken = passwordService.generateSecureToken();
        String hashedToken = passwordService.hashToken(rawActivationToken);

        // Save user
        User newUser = new User(request.email(), hashedPassword, hashedToken);
        User savedUser = userRepository.save(newUser);

        // Simulate sending email:
        System.out.println("-------------------------------------------------------------");
        System.out.println("ACTIVATION EMAIL SIMULATION:");
        System.out.println("To: " + savedUser.getEmail());
        System.out.println("Activation Link: https://localhost:8443/api/activate?token=" + rawActivationToken + "&email=" + savedUser.getEmail());
        System.out.println("Stored SHA-256 Hash in DB: " + hashedToken);
        System.out.println("-------------------------------------------------------------");

        return ResponseEntity.ok("User registered successfully! DB ID: " + savedUser.getId());
    }
}