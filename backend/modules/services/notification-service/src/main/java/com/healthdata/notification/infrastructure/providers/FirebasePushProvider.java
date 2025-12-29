package com.healthdata.notification.infrastructure.providers;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) push notification provider.
 *
 * Supports:
 * - Device-specific notifications
 * - Topic-based notifications
 * - Silent/data-only notifications
 * - Custom data payloads
 * - Android and iOS platforms
 */
@Component
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "firebase")
@RequiredArgsConstructor
@Slf4j
public class FirebasePushProvider implements PushProvider {

    private final FirebasePushProperties properties;

    private volatile boolean initialized = false;
    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    public void init() {
        if (properties.getCredentialsJson() == null || properties.getCredentialsJson().isEmpty()) {
            log.warn("Firebase credentials not configured, push provider disabled");
            return;
        }

        try {
            // Initialize Firebase with credentials JSON
            InputStream credentialsStream = new ByteArrayInputStream(
                    properties.getCredentialsJson().getBytes(StandardCharsets.UTF_8)
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .setProjectId(properties.getProjectId())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firebaseMessaging = FirebaseMessaging.getInstance();
            initialized = true;
            log.info("Firebase push provider initialized successfully for project: {}",
                properties.getProjectId());

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
            initialized = false;
        }
    }

    @Override
    @Retry(name = "firebase-push")
    @CircuitBreaker(name = "firebase-push", fallbackMethod = "sendFallback")
    public String send(String deviceToken, String title, String body) {
        return sendWithData(deviceToken, title, body, Map.of());
    }

    @Override
    @Retry(name = "firebase-push")
    @CircuitBreaker(name = "firebase-push", fallbackMethod = "sendWithDataFallback")
    public String sendWithData(String deviceToken, String title, String body, Map<String, String> data) {
        if (!isAvailable()) {
            throw new PushProviderException("Firebase push provider not available");
        }

        try {
            log.debug("Sending push notification to device: {}", maskToken(deviceToken));

            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Add Android-specific config
            messageBuilder.setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setIcon(properties.getAndroidIcon())
                            .setColor(properties.getAndroidColor())
                            .setChannelId(properties.getAndroidChannelId())
                            .build())
                    .build());

            // Add iOS-specific config
            messageBuilder.setApnsConfig(ApnsConfig.builder()
                    .putHeader("apns-priority", "10")
                    .setAps(Aps.builder()
                            .setBadge(1)
                            .setSound("default")
                            .build())
                    .build());

            // Add custom data payload
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String messageId = firebaseMessaging.send(messageBuilder.build());
            log.info("Push notification sent successfully, message ID: {}", messageId);

            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Firebase messaging error: {} - {}", e.getMessagingErrorCode(), e.getMessage());
            throw new PushProviderException("Firebase error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to send push notification: {}", e.getMessage());
            throw new PushProviderException("Failed to send push: " + e.getMessage(), e);
        }
    }

    @Override
    @Retry(name = "firebase-push")
    @CircuitBreaker(name = "firebase-push", fallbackMethod = "sendToTopicFallback")
    public String sendToTopic(String topic, String title, String body) {
        if (!isAvailable()) {
            throw new PushProviderException("Firebase push provider not available");
        }

        try {
            log.debug("Sending push notification to topic: {}", topic);

            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("Topic notification sent successfully, message ID: {}", messageId);

            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Firebase topic error: {} - {}", e.getMessagingErrorCode(), e.getMessage());
            throw new PushProviderException("Firebase error: " + e.getMessage(), e);
        }
    }

    @Override
    @Retry(name = "firebase-push")
    @CircuitBreaker(name = "firebase-push", fallbackMethod = "sendSilentFallback")
    public String sendSilent(String deviceToken, Map<String, String> data) {
        if (!isAvailable()) {
            throw new PushProviderException("Firebase push provider not available");
        }

        try {
            log.debug("Sending silent push notification to device: {}", maskToken(deviceToken));

            Message message = Message.builder()
                    .setToken(deviceToken)
                    .putAllData(data)
                    // Android: data-only message
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    // iOS: content-available for background processing
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setContentAvailable(true)
                                    .build())
                            .build())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("Silent push sent successfully, message ID: {}", messageId);

            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Firebase silent push error: {}", e.getMessage());
            throw new PushProviderException("Firebase error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return initialized && properties.isEnabled();
    }

    @Override
    public String getProviderName() {
        return "firebase";
    }

    // Fallback methods

    public String sendFallback(String deviceToken, String title, String body, Throwable t) {
        log.warn("Push send fallback triggered: {}", t.getMessage());
        throw new PushProviderException("Push service temporarily unavailable", t);
    }

    public String sendWithDataFallback(String deviceToken, String title, String body,
            Map<String, String> data, Throwable t) {
        log.warn("Push send fallback triggered: {}", t.getMessage());
        throw new PushProviderException("Push service temporarily unavailable", t);
    }

    public String sendToTopicFallback(String topic, String title, String body, Throwable t) {
        log.warn("Push topic fallback triggered: {}", t.getMessage());
        throw new PushProviderException("Push service temporarily unavailable", t);
    }

    public String sendSilentFallback(String deviceToken, Map<String, String> data, Throwable t) {
        log.warn("Silent push fallback triggered: {}", t.getMessage());
        throw new PushProviderException("Push service temporarily unavailable", t);
    }

    /**
     * Mask device token for logging.
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    /**
     * Exception for push provider errors.
     */
    public static class PushProviderException extends RuntimeException {
        public PushProviderException(String message) {
            super(message);
        }

        public PushProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
