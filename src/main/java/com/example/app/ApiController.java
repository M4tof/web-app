package com.example.app;

import com.example.app.registration.CaptchaService;
import com.example.app.registration.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import com.example.app.registration.RateLimitingService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
public class ApiController {

    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    private final RateLimitingService rateLimitingService;
    private final UserLogService userLogService;

    public ApiController(UserRepository userRepository,
                         CaptchaService captchaService,
                         PasswordService passwordService,
                         EmailVerificationService emailVerificationService,
                         RateLimitingService rateLimitingService,
                         UserLogService userLogService) {
        this.userRepository = userRepository;
        this.captchaService = captchaService;
        this.passwordService = passwordService;
        this.emailVerificationService = emailVerificationService;
        this.rateLimitingService = rateLimitingService;
        this.userLogService = userLogService;
    }

    @GetMapping("/api/captcha")
    public CaptchaService.CaptchaResponse getCaptcha() {
        return captchaService.generateCaptcha();
    }

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

        String clientIp = RateLimitingService.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. Rate Limiter
        long blockedSeconds = rateLimitingService.checkRateLimit(clientIp, userAgent);
        if (blockedSeconds > 0) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(blockedSeconds))
                    .body("Too many registration attempts. Please wait " + blockedSeconds + " seconds.");
        }

        // 2. Honeypot Check
        if (request.nickname() != null && !request.nickname().isBlank()) {
            System.out.println("HONEYPOT TRIGGERED from IP: " + clientIp);
            return ResponseEntity.ok("Registration successful! Check your email to activate your account.");
        }

        // 3. Password Length Cap
        if (request.password() == null || request.password().getBytes(StandardCharsets.UTF_8).length > 256) {
            return ResponseEntity.badRequest().body("Password exceeds maximum allowed length.");
        }

        // 4. CAPTCHA Verification
        if (!captchaService.validateCaptcha(request.captchaId(), request.captchaAnswer())) {
            return ResponseEntity.badRequest().body("Captcha verification failed. Please try again.");
        }

        // 5. DNS MX Record Lookup
        if (!emailVerificationService.hasValidMxRecord(request.email())) {
            return ResponseEntity.badRequest().body("Invalid email: Domain does not accept mail (no MX record).");
        }

        // Crypto & Token Generation
        String hashedPassword = passwordService.hashPassword(request.password());
        String rawToken = passwordService.generateSecureToken();
        String hashedToken = passwordService.hashToken(rawToken);

        User newUser = new User(request.email().toLowerCase().trim(), hashedPassword, hashedToken);

        try {
            User savedUser = userRepository.saveAndFlush(newUser);

            userLogService.logEvent(savedUser.getId(), "REGISTRATION_SUCCESS", clientIp, userAgent);

            System.out.println("-------------------------------------------------------------");
            System.out.println("DELIVERY CONFIRMED: Activation link sent to " + savedUser.getEmail());
            // Updated link to /activate:
            System.out.println("Link: https://localhost:8443/activate?token=" + rawToken + "&email=" + savedUser.getEmail());
            System.out.println("-------------------------------------------------------------");

            return ResponseEntity.ok("Registration successful! Check your email to activate your account.");

        } catch (DataIntegrityViolationException ex) {
            System.out.println("DUPLICATE CAUGHT: Email [" + request.email() + "] is already in users table.");
            userLogService.logEvent(null, "DUPLICATE_REG_ATTEMPT", clientIp, userAgent);
            return ResponseEntity.ok("Registration successful! Check your email to activate your account.");
        }
    }

    // ==============================================================
    // NOW MAPPED DIRECTLY TO /activate
    // ==============================================================
    @GetMapping("/activate")
    public ResponseEntity<String> handleActivation(
            @RequestParam("token") String token,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {

        String clientIp = RateLimitingService.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (token == null || token.isBlank() || email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Missing token or email parameter.");
        }

        // 1. Compute T_hash = SHA-256(T_raw)
        String hashedToken = passwordService.hashToken(token.trim());

        // 2. Query PostgreSQL
        Optional<User> userOpt = userRepository.findByEmailAndActivationTokenHash(email.trim().toLowerCase(), hashedToken);

        // Branch 1: Invalid Token or Already Active -> HTTP 400
        if (userOpt.isEmpty()) {
            userLogService.logEvent(null, "ACTIVATION_INVALID_TOKEN", clientIp, userAgent);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid activation link or account is already active.");
        }

        User user = userOpt.get();

        // Branch 2: Token Expired -> HTTP 410 Gone
        if (user.getActivationExpiresAt() != null && java.time.Instant.now().isAfter(user.getActivationExpiresAt())) {
            userLogService.logEvent(user.getId(), "ACTIVATION_EXPIRED_TOKEN", clientIp, userAgent);
            return ResponseEntity
                    .status(HttpStatus.GONE)
                    .body("Activation link has expired.");
        }

        // Branch 3: Success -> Update & HTTP 200 OK
        user.setActivated(true);
        user.setActivationTokenHash(null);
        user.setActivationExpiresAt(null);
        userRepository.save(user);

        userLogService.logEvent(user.getId(), "ACTIVATION_SUCCESS", clientIp, userAgent);
        System.out.println("ACTIVATION SUCCESS: User #" + user.getId() + " activated.");

        return ResponseEntity.ok("Account successfully activated! Proceed to Login.");
    }
}