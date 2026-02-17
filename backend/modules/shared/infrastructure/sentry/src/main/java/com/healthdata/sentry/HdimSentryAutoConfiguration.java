package com.healthdata.sentry;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared Sentry configuration for backend services.
 *
 * Provides a HIPAA-oriented BeforeSend callback that removes likely PHI/PII
 * from Sentry events before they leave the service boundary.
 */
@AutoConfiguration
@ConditionalOnClass(SentryOptions.class)
public class HdimSentryAutoConfiguration {

    private static final Set<String> REQUEST_HEADER_ALLOWLIST = Set.of(
        "accept",
        "accept-encoding",
        "accept-language",
        "content-type",
        "host",
        "user-agent",
        "x-request-id",
        "x-correlation-id"
    );

    private static final List<String> SENSITIVE_KEY_MARKERS = List.of(
        "auth",
        "token",
        "secret",
        "password",
        "cookie",
        "session",
        "ssn",
        "dob",
        "birth",
        "phone",
        "email",
        "address",
        "patient",
        "member",
        "mrn",
        "npi"
    );

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "hdim.sentry.phi-filter.enabled", havingValue = "true", matchIfMissing = true)
    public SentryOptions.BeforeSendCallback hdimSentryBeforeSendCallback(
        @Value("${hdim.sentry.phi-filter.redaction-text:[REDACTED]}") String redactionText
    ) {
        return (event, hint) -> sanitizeEvent(event, hint, redactionText);
    }

    private SentryEvent sanitizeEvent(SentryEvent event, Hint hint, String redactionText) {
        sanitizeRequest(event, redactionText);
        sanitizeUser(event);
        sanitizeObjectMap(event.getExtras(), redactionText);
        sanitizeStringMap(event.getTags(), redactionText);
        sanitizeBreadcrumbs(event.getBreadcrumbs(), redactionText);

        event.setTag("phi_filtered", "true");
        return event;
    }

    private void sanitizeRequest(SentryEvent event, String redactionText) {
        Request request = event.getRequest();
        if (request == null) {
            return;
        }

        request.setData(redactionText);
        request.setCookies(null);
        request.setQueryString(null);
        request.setEnvs(Collections.emptyMap());
        request.setOthers(Collections.emptyMap());

        Map<String, String> headers = request.getHeaders();
        if (headers == null || headers.isEmpty()) {
            request.setHeaders(Collections.emptyMap());
            return;
        }

        Map<String, String> sanitizedHeaders = new HashMap<>();
        headers.forEach((key, value) -> {
            String lowerKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (REQUEST_HEADER_ALLOWLIST.contains(lowerKey)) {
                sanitizedHeaders.put(key, truncate(value, 256));
            } else {
                sanitizedHeaders.put(key, redactionText);
            }
        });
        request.setHeaders(sanitizedHeaders);
    }

    private void sanitizeUser(SentryEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }

        user.setEmail(null);
        user.setIpAddress(null);
        user.setName(null);
        user.setUsername(null);
        user.setId(null);
        user.setData(Collections.emptyMap());
    }

    private void sanitizeBreadcrumbs(List<Breadcrumb> breadcrumbs, String redactionText) {
        if (breadcrumbs == null || breadcrumbs.isEmpty()) {
            return;
        }

        for (Breadcrumb breadcrumb : breadcrumbs) {
            if (breadcrumb == null || breadcrumb.getData() == null) {
                continue;
            }

            Map<String, Object> data = breadcrumb.getData();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (isSensitiveKey(entry.getKey())) {
                    breadcrumb.setData(entry.getKey(), redactionText);
                } else if (entry.getValue() instanceof String value) {
                    breadcrumb.setData(entry.getKey(), truncate(value, 256));
                }
            }
        }
    }

    private void sanitizeStringMap(Map<String, String> map, String redactionText) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (String key : List.copyOf(map.keySet())) {
            if (isSensitiveKey(key)) {
                map.put(key, redactionText);
            }
        }
    }

    private void sanitizeObjectMap(Map<String, Object> map, String redactionText) {
        if (map == null || map.isEmpty()) {
            return;
        }

        for (String key : List.copyOf(map.keySet())) {
            if (isSensitiveKey(key)) {
                map.put(key, redactionText);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        String lower = key.toLowerCase(Locale.ROOT);
        for (String marker : SENSITIVE_KEY_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
