package com.example.app;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private final SecureRandom random = new SecureRandom();

    // Cache active challenges: id -> { answer, expiration }
    private final Map<String, CaptchaChallenge> store = new ConcurrentHashMap<>();

    private record CaptchaChallenge(int answer, Instant expiresAt) {}

    public record CaptchaResponse(String captchaId, String prompt, String imageBase64) {}

    // Pre-loaded in-memory so we don't read the disk on every request
    private BufferedImage backgroundImage;
    private BufferedImage dogImage;

    @PostConstruct
    public void loadAssets() {
        try {
            backgroundImage = ImageIO.read(new ClassPathResource("captcha/background.png").getInputStream());
            dogImage = ImageIO.read(new ClassPathResource("captcha/dog.png").getInputStream());

            if (backgroundImage == null || dogImage == null) {
                throw new IllegalStateException("background.png or dog.png could not be loaded!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load captcha images from resources/captcha/", e);
        }
    }

    public CaptchaResponse generateCaptcha() {
        // 1. Pick a count from 1 to 9
        int count = 1 + random.nextInt(9);
        String captchaId = UUID.randomUUID().toString();

        // Expire in 2 minutes
        store.put(captchaId, new CaptchaChallenge(count, Instant.now().plusSeconds(120)));

        // 2. Generate composite image
        String imageBase64 = composeImage(count);

        return new CaptchaResponse(
                captchaId,
                "How many dogs are in the picture?",
                imageBase64
        );
    }

    private String composeImage(int count) {
        int bgWidth = backgroundImage.getWidth();
        int bgHeight = backgroundImage.getHeight();

        // Create a new canvas copy of the background
        BufferedImage composite = new BufferedImage(bgWidth, bgHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = composite.createGraphics();

        // Enable smooth rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Draw background
        g.drawImage(backgroundImage, 0, 0, null);

        // 3. Grid allocation (3 cols x 3 rows = 9 slots)
        List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        Collections.shuffle(slots, random); // Pick random distinct slots

        int cellWidth = bgWidth / 3;
        int cellHeight = bgHeight / 3;

        // Target dog size: roughly fit inside a cell with breathing room
        int targetDogWidth = (int) (cellWidth * 0.65);
        int targetDogHeight = (int) (dogImage.getHeight() * ((double) targetDogWidth / dogImage.getWidth()));

        for (int i = 0; i < count; i++) {
            int slot = slots.get(i);
            int col = slot % 3;
            int row = slot / 3;

            // Base top-left of the cell
            int cellX = col * cellWidth;
            int cellY = row * cellHeight;

            // Random jitter within the cell
            int jitterX = random.nextInt(Math.max(1, cellWidth - targetDogWidth));
            int jitterY = random.nextInt(Math.max(1, cellHeight - targetDogHeight));

            int posX = cellX + jitterX;
            int posY = cellY + jitterY;

            // Random slight scaling (85% to 115%)
            double scale = 0.85 + (random.nextDouble() * 0.30);
            int scaledW = (int) (targetDogWidth * scale);
            int scaledH = (int) (targetDogHeight * scale);

            // 50% chance to flip horizontally
            boolean flip = random.nextBoolean();

            AffineTransform at = new AffineTransform();
            at.translate(posX, posY);
            if (flip) {
                at.translate(scaledW, 0);
                at.scale(-1, 1);
            }
            at.scale((double) scaledW / dogImage.getWidth(), (double) scaledH / dogImage.getHeight());

            g.drawImage(dogImage, at, null);
        }

        g.dispose();

        // Convert to Base64 PNG
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(composite, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to render captcha image", e);
        }
    }

    public boolean validateCaptcha(String captchaId, String answerStr) {
        if (captchaId == null || answerStr == null) return false;

        CaptchaChallenge challenge = store.remove(captchaId); // Single-use
        if (challenge == null || Instant.now().isAfter(challenge.expiresAt())) {
            return false;
        }

        try {
            int answer = Integer.parseInt(answerStr.trim());
            return answer == challenge.answer();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}