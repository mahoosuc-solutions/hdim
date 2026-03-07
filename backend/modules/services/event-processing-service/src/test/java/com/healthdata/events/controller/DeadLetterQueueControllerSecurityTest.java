package com.healthdata.events.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeadLetterQueueController Security Contract Tests")
class DeadLetterQueueControllerSecurityTest {

    @Test
    @DisplayName("All endpoint methods should declare @PreAuthorize")
    void allEndpointsShouldDeclarePreAuthorize() {
        List<String> endpointMethods = List.of(
                "getFailedEvents",
                "getFailedEventsByPatient",
                "getFailedEventsByTopic",
                "getExhausted",
                "getRecentFailures",
                "getStats",
                "retryEvent",
                "resolveEvent",
                "exhaustEvent"
        );

        for (String methodName : endpointMethods) {
            Method method = findMethodByName(methodName);
            assertThat(method.getAnnotation(PreAuthorize.class))
                    .as("Method %s must have @PreAuthorize", methodName)
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("Tenant-scoped endpoint methods should require X-Tenant-ID request header")
    void allEndpointsShouldRequireTenantHeader() {
        List<String> endpointMethods = List.of(
                "getFailedEvents",
                "getFailedEventsByPatient",
                "getFailedEventsByTopic",
                "getExhausted",
                "getRecentFailures",
                "getStats",
                "retryEvent",
                "resolveEvent",
                "exhaustEvent"
        );

        for (String methodName : endpointMethods) {
            Method method = findMethodByName(methodName);
            boolean hasTenantHeader = false;
            for (Parameter parameter : method.getParameters()) {
                RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
                if (requestHeader != null && "X-Tenant-ID".equals(requestHeader.value())) {
                    hasTenantHeader = true;
                    break;
                }
            }

            assertThat(hasTenantHeader)
                    .as("Method %s must require @RequestHeader(\"X-Tenant-ID\")", methodName)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Mutation endpoints should require elevated roles and exclude AUDITOR")
    void mutationEndpointsShouldExcludeAuditorRole() {
        List<String> mutationMethods = List.of("retryEvent", "resolveEvent", "exhaustEvent");

        for (String methodName : mutationMethods) {
            Method method = findMethodByName(methodName);
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            assertThat(preAuthorize).isNotNull();
            assertThat(preAuthorize.value()).contains("ADMIN");
            assertThat(preAuthorize.value()).doesNotContain("AUDITOR");
        }
    }

    private Method findMethodByName(String name) {
        return List.of(DeadLetterQueueController.class.getDeclaredMethods()).stream()
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + name));
    }
}
