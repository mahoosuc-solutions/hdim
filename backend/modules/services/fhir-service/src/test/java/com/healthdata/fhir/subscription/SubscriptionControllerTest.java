package com.healthdata.fhir.subscription;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Controller Tests")
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SubscriptionController controller = new SubscriptionController(subscriptionService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create subscription")
    void shouldCreateSubscription() throws Exception {
        SubscriptionController.SubscriptionRequest request = buildRequest();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria(request.getCriteria())
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .resourceJson("{}")
                .build();
        when(subscriptionService.createSubscription(any(FhirSubscription.class))).thenReturn(subscription);

        mockMvc.perform(post("/api/v1/Subscription")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("Patient")));
    }

    @Test
    @DisplayName("Should return validation error on create")
    void shouldReturnValidationErrorOnCreate() throws Exception {
        SubscriptionController.SubscriptionRequest request = buildRequest();
        when(subscriptionService.createSubscription(any(FhirSubscription.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(post("/api/v1/Subscription")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("validation_error")));
    }

    @Test
    @DisplayName("Should get subscription")
    void shouldGetSubscription() throws Exception {
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .resourceJson("{}")
                .build();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.of(subscription));

        mockMvc.perform(get("/api/v1/Subscription/{id}", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Patient")));
    }

    @Test
    @DisplayName("Should return not found for missing subscription")
    void shouldReturnNotFound() throws Exception {
        when(subscriptionService.getSubscription(any(UUID.class), eq("tenant-1"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/Subscription/{id}", UUID.randomUUID())
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update subscription")
    void shouldUpdateSubscription() throws Exception {
        SubscriptionController.SubscriptionRequest request = buildRequest();
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria(request.getCriteria())
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .resourceJson("{}")
                .build();
        when(subscriptionService.updateSubscription(eq(id), any(FhirSubscription.class), eq("tenant-1")))
                .thenReturn(subscription);

        mockMvc.perform(put("/api/v1/Subscription/{id}", id)
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return not found on update when missing")
    void shouldReturnNotFoundOnUpdateWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new SubscriptionService.SubscriptionNotFoundException(id))
                .when(subscriptionService).updateSubscription(eq(id), any(FhirSubscription.class), eq("tenant-1"));

        mockMvc.perform(put("/api/v1/Subscription/{id}", id)
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("not_found")));
    }

    @Test
    @DisplayName("Should delete subscription")
    void shouldDeleteSubscription() throws Exception {
        mockMvc.perform(delete("/api/v1/Subscription/{id}", UUID.randomUUID())
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should list subscriptions")
    void shouldListSubscriptions() throws Exception {
        FhirSubscription active = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .resourceJson("{}")
                .build();
        FhirSubscription off = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .status(FhirSubscription.SubscriptionStatus.OFF)
                .resourceJson("{}")
                .build();
        when(subscriptionService.getSubscriptions("tenant-1")).thenReturn(List.of(active, off));

        mockMvc.perform(get("/api/v1/Subscription")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ACTIVE")))
                .andExpect(content().string(containsString("OFF")));
    }

    @Test
    @DisplayName("Should filter subscriptions by status")
    void shouldFilterSubscriptionsByStatus() throws Exception {
        FhirSubscription active = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .resourceJson("{}")
                .build();
        when(subscriptionService.getSubscriptions("tenant-1")).thenReturn(List.of(active));

        mockMvc.perform(get("/api/v1/Subscription")
                        .header("X-Tenant-ID", "tenant-1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ACTIVE")));
    }

    @Test
    @DisplayName("Should activate and deactivate subscription")
    void shouldActivateAndDeactivateSubscription() throws Exception {
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .resourceJson("{}")
                .build();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.of(subscription));

        mockMvc.perform(post("/api/v1/Subscription/{id}/$activate", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk());
        verify(subscriptionService).activateSubscription(id);

        mockMvc.perform(post("/api/v1/Subscription/{id}/$deactivate", id)
                        .header("X-Tenant-ID", "tenant-1")
                        .param("reason", "paused"))
                .andExpect(status().isOk());
        verify(subscriptionService).deactivateSubscription(id, "paused");
    }

    @Test
    @DisplayName("Should return not found on activate when missing")
    void shouldReturnNotFoundOnActivateWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/Subscription/{id}/$activate", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("not_found")));
    }

    @Test
    @DisplayName("Should return not found on deactivate when missing")
    void shouldReturnNotFoundOnDeactivateWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/Subscription/{id}/$deactivate", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("not_found")));
    }

    @Test
    @DisplayName("Should return status for subscription")
    void shouldReturnStatus() throws Exception {
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .notificationCount(2)
                .errorCount(0)
                .resourceJson("{}")
                .build();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.of(subscription));

        mockMvc.perform(get("/api/v1/Subscription/{id}/$status", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ACTIVE")));
    }

    @Test
    @DisplayName("Should return not found on status when missing")
    void shouldReturnNotFoundOnStatus() throws Exception {
        UUID id = UUID.randomUUID();
        when(subscriptionService.getSubscription(id, "tenant-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/Subscription/{id}/$status", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle not found exception")
    void shouldHandleNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new SubscriptionService.SubscriptionNotFoundException(id))
                .when(subscriptionService).deleteSubscription(id, "tenant-1");

        mockMvc.perform(delete("/api/v1/Subscription/{id}", id)
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("not_found")));
    }

    private SubscriptionController.SubscriptionRequest buildRequest() {
        SubscriptionController.SubscriptionRequest request = new SubscriptionController.SubscriptionRequest();
        SubscriptionController.SubscriptionRequest.ChannelRequest channel = new SubscriptionController.SubscriptionRequest.ChannelRequest();
        channel.setType(FhirSubscription.ChannelType.REST_HOOK);
        channel.setEndpoint("http://callback");
        request.setCriteria("Patient?status=active");
        request.setChannel(channel);
        return request;
    }
}
