package com.example.app;

import com.example.app.registration.CaptchaService;
import com.example.app.registration.EmailVerificationService;
import com.example.app.registration.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    private final RateLimitingService rateLimitingService; // 1. Inject

    public ApiController(UserRepository userRepository,
                         CaptchaService captchaService,
                         PasswordService passwordService,
                         EmailVerificationService emailVerificationService,
                         RateLimitingService rateLimitingService) {
        this.userRepository = userRepository;
        this.captchaService = captchaService;
        this.passwordService = passwordService;
        this.emailVerificationService = emailVerificationService;
        this.rateLimitingService = rateLimitingService;
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
    public ResponseEntity<String> handleRegistration(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        // 0. RATE LIMITING CHECK (FIRST LINE OF DEFENSE)
        String clientIp = RateLimitingService.getClientIp(httpRequest);
        long blockedSeconds = rateLimitingService.checkRateLimit(clientIp);

        if (blockedSeconds > 0) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS) // HTTP 429
                    .header("Retry-After", String.valueOf(blockedSeconds))
                    .body("Too many registration attempts. Please wait " + blockedSeconds + " seconds before trying again.");
        }

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

        // DNS MX Deliverability Check
//        if (!emailVerificationService.hasValidMxRecord(request.email())) {
//            return ResponseEntity.badRequest().body("Invalid email: Domain does not exist or cannot receive emails.");
//        }

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