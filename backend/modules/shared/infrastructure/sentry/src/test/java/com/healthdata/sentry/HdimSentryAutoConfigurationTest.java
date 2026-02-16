package com.healthdata.sentry;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HdimSentryAutoConfigurationTest {

    @Test
    void beforeSendCallbackRedactsSensitiveFields() {
        HdimSentryAutoConfiguration configuration = new HdimSentryAutoConfiguration();
        SentryOptions.BeforeSendCallback callback = configuration.hdimSentryBeforeSendCallback("[REDACTED]");

        SentryEvent event = new SentryEvent();

        Request request = new Request();
        request.setData("patient details and notes");
        request.setCookies("session=abc123");
        request.setQueryString("patientId=123");
        request.setHeaders(Map.of(
            "Authorization", "Bearer token",
            "User-Agent", "JUnit"
        ));
        event.setRequest(request);

        User user = new User();
        user.setEmail("jane@example.com");
        user.setId("patient-123");
        user.setIpAddress("127.0.0.1");
        event.setUser(user);

        Map<String, Object> extras = new HashMap<>();
        extras.put("patientName", "Jane Doe");
        extras.put("safeKey", "kept");
        event.setExtras(extras);

        Map<String, String> tags = new HashMap<>();
        tags.put("authStatus", "ok");
        tags.put("service", "patient-service");
        event.setTags(tags);

        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setData("email", "jane@example.com");
        breadcrumb.setData("operation", "load-patient");
        event.setBreadcrumbs(List.of(breadcrumb));

        SentryEvent sanitized = callback.execute(event, new Hint());

        assertThat(sanitized).isNotNull();
        assertThat(sanitized.getTag("phi_filtered")).isEqualTo("true");

        Request sanitizedRequest = sanitized.getRequest();
        assertThat(sanitizedRequest).isNotNull();
        assertThat(sanitizedRequest.getData()).isEqualTo("[REDACTED]");
        assertThat(sanitizedRequest.getCookies()).isNull();
        assertThat(sanitizedRequest.getQueryString()).isNull();
        assertThat(sanitizedRequest.getHeaders().get("Authorization")).isEqualTo("[REDACTED]");
        assertThat(sanitizedRequest.getHeaders().get("User-Agent")).isEqualTo("JUnit");

        User sanitizedUser = sanitized.getUser();
        assertThat(sanitizedUser).isNotNull();
        assertThat(sanitizedUser.getEmail()).isNull();
        assertThat(sanitizedUser.getId()).isNull();
        assertThat(sanitizedUser.getIpAddress()).isNull();

        assertThat(sanitized.getExtras().get("patientName")).isEqualTo("[REDACTED]");
        assertThat(sanitized.getExtras().get("safeKey")).isEqualTo("kept");
        assertThat(sanitized.getTags().get("authStatus")).isEqualTo("[REDACTED]");
        assertThat(sanitized.getTags().get("service")).isEqualTo("patient-service");

        Breadcrumb sanitizedBreadcrumb = sanitized.getBreadcrumbs().get(0);
        assertThat(sanitizedBreadcrumb.getData("email")).isEqualTo("[REDACTED]");
        assertThat(sanitizedBreadcrumb.getData("operation")).isEqualTo("load-patient");
    }
}
