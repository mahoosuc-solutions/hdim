package com.healthdata.sales.client;

import com.healthdata.sales.config.ZohoConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Zoho Bookings API Client
 * Meeting scheduler for investor demos and discovery calls
 *
 * Features:
 * - Service/meeting type management
 * - Availability checking
 * - Appointment booking
 * - Automatic Zoho Meeting link generation
 * - Email/SMS reminders
 * - CRM integration (auto-update deal stage)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZohoBookingsClient {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;
    private final WebClient.Builder webClientBuilder;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ==================== Availability ====================

    /**
     * Get available time slots for a service on a given date
     */
    @CircuitBreaker(name = "zohoService", fallbackMethod = "getAvailabilityFallback")
    @Retry(name = "zohoService")
    public List<TimeSlot> getAvailability(String serviceId, LocalDate date, String staffId) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            String effectiveStaffId = staffId != null ? staffId : 
                zohoConfig.getBookings().getDefaultStaffId();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/availability")
                    .queryParam("service_id", serviceId)
                    .queryParam("staff_id", effectiveStaffId)
                    .queryParam("selected_date", date.format(DATE_FORMAT))
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> slots = (List<Map<String, Object>>) data.get("slots");
                
                return slots != null ? slots.stream()
                    .map(this::mapToTimeSlot)
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get availability: {}", e.getMessage());
        }
        return List.of();
    }

    public List<TimeSlot> getAvailabilityFallback(String serviceId, LocalDate date, 
                                                   String staffId, Throwable t) {
        log.warn("Zoho Bookings availability fallback triggered: {}", t.getMessage());
        return List.of();
    }

    /**
     * Get available dates for a service within a date range
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<LocalDate> getAvailableDates(String serviceId, LocalDate startDate, 
                                              LocalDate endDate, String staffId) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            String effectiveStaffId = staffId != null ? staffId : 
                zohoConfig.getBookings().getDefaultStaffId();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/available_dates")
                    .queryParam("service_id", serviceId)
                    .queryParam("staff_id", effectiveStaffId)
                    .queryParam("from_date", startDate.format(DATE_FORMAT))
                    .queryParam("to_date", endDate.format(DATE_FORMAT))
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<String> dates = (List<String>) data.get("available_dates");
                
                return dates != null ? dates.stream()
                    .map(d -> LocalDate.parse(d, DATE_FORMAT))
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get available dates: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Appointment Booking ====================

    /**
     * Book an appointment
     */
    @CircuitBreaker(name = "zohoService", fallbackMethod = "bookAppointmentFallback")
    @Retry(name = "zohoService")
    public BookingResult bookAppointment(BookingRequest request) {
        if (!isEnabled()) {
            return BookingResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("service_id", request.getServiceId());
            bookingData.put("staff_id", request.getStaffId() != null ? 
                request.getStaffId() : zohoConfig.getBookings().getDefaultStaffId());
            bookingData.put("from_time", request.getStartTime().format(DATETIME_FORMAT));
            
            // Customer info
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("name", request.getCustomerName());
            customerInfo.put("email", request.getCustomerEmail());
            if (request.getCustomerPhone() != null) {
                customerInfo.put("phone_number", request.getCustomerPhone());
            }
            bookingData.put("customer", customerInfo);

            // Additional notes
            if (request.getNotes() != null) {
                bookingData.put("notes", request.getNotes());
            }

            // CRM integration - link to deal
            if (request.getCrmDealId() != null) {
                bookingData.put("crm_deal_id", request.getCrmDealId());
            }

            // Custom fields for investor context
            if (request.getCustomFields() != null) {
                bookingData.put("custom_fields", request.getCustomFields());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri("/appointment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookingData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                
                BookingResult result = BookingResult.success();
                result.setBookingId((String) data.get("booking_id"));
                result.setConfirmationNumber((String) data.get("confirmation_number"));
                result.setMeetingLink((String) data.get("meeting_link"));
                result.setCalendarLink((String) data.get("calendar_link"));
                
                log.info("Booked appointment {} for {}", 
                    result.getBookingId(), request.getCustomerEmail());
                return result;
            }
            
            return BookingResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to book appointment: {}", e.getMessage());
            return BookingResult.failed(e.getMessage());
        }
    }

    public BookingResult bookAppointmentFallback(BookingRequest request, Throwable t) {
        log.warn("Zoho Bookings appointment fallback triggered: {}", t.getMessage());
        return BookingResult.failed("Circuit breaker open: " + t.getMessage());
    }

    /**
     * Reschedule an appointment
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public BookingResult rescheduleAppointment(String bookingId, LocalDateTime newStartTime) {
        if (!isEnabled()) {
            return BookingResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> rescheduleData = new HashMap<>();
            rescheduleData.put("from_time", newStartTime.format(DATETIME_FORMAT));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.put()
                .uri("/appointment/" + bookingId + "/reschedule")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rescheduleData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                log.info("Rescheduled appointment {} to {}", bookingId, newStartTime);
                return BookingResult.success();
            }
            
            return BookingResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to reschedule appointment: {}", e.getMessage());
            return BookingResult.failed(e.getMessage());
        }
    }

    /**
     * Cancel an appointment
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public BookingResult cancelAppointment(String bookingId, String reason) {
        if (!isEnabled()) {
            return BookingResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> cancelData = new HashMap<>();
            if (reason != null) {
                cancelData.put("cancellation_reason", reason);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.put()
                .uri("/appointment/" + bookingId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cancelData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "success".equals(response.get("status"))) {
                log.info("Cancelled appointment {}", bookingId);
                return BookingResult.success();
            }
            
            return BookingResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to cancel appointment: {}", e.getMessage());
            return BookingResult.failed(e.getMessage());
        }
    }

    // ==================== Appointment Retrieval ====================

    /**
     * Get appointment details
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public AppointmentDetails getAppointment(String bookingId) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri("/appointment/" + bookingId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return mapToAppointmentDetails(data);
            }
        } catch (Exception e) {
            log.error("Failed to get appointment: {}", e.getMessage());
        }
        return null;
    }

    /**
     * List appointments for a date range
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<AppointmentDetails> listAppointments(LocalDate startDate, LocalDate endDate, 
                                                      String staffId, String status) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/appointments")
                        .queryParam("from_date", startDate.format(DATE_FORMAT))
                        .queryParam("to_date", endDate.format(DATE_FORMAT));
                    if (staffId != null) {
                        builder.queryParam("staff_id", staffId);
                    }
                    if (status != null) {
                        builder.queryParam("status", status);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> appointments = 
                    (List<Map<String, Object>>) data.get("appointments");
                
                return appointments != null ? appointments.stream()
                    .map(this::mapToAppointmentDetails)
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to list appointments: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Get upcoming appointments for a customer
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<AppointmentDetails> getCustomerAppointments(String customerEmail) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/customer/appointments")
                    .queryParam("email", customerEmail)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> appointments = 
                    (List<Map<String, Object>>) data.get("appointments");
                
                return appointments != null ? appointments.stream()
                    .map(this::mapToAppointmentDetails)
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get customer appointments: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Service Management ====================

    /**
     * List all available services (meeting types)
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<ServiceInfo> getServices() {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri("/services")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> services = 
                    (List<Map<String, Object>>) data.get("services");
                
                return services != null ? services.stream()
                    .map(this::mapToServiceInfo)
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get services: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Booking Link Generation ====================

    /**
     * Generate a direct booking link for a service
     */
    public String generateBookingLink(String serviceId, String staffId) {
        // Use configured datacenter for correct URL
        String datacenter = zohoConfig.getApi().getDatacenter();
        String baseBookingUrl = "https://bookings.zoho." + datacenter;
        
        StringBuilder url = new StringBuilder(baseBookingUrl);
        url.append("/").append(zohoConfig.getBookings().getWorkspaceId());
        url.append("/").append(serviceId);
        
        if (staffId != null) {
            url.append("?staff=").append(staffId);
        }
        
        return url.toString();
    }

    /**
     * Generate booking link for investor demo
     */
    public String generateDemoBookingLink() {
        return generateBookingLink(
            zohoConfig.getBookings().getMeetingTypes().getProductDemo(),
            null
        );
    }

    /**
     * Generate booking link for discovery call
     */
    public String generateDiscoveryCallLink() {
        return generateBookingLink(
            zohoConfig.getBookings().getMeetingTypes().getDiscoveryCall(),
            null
        );
    }

    // ==================== Helper Methods ====================

    private boolean isEnabled() {
        return zohoConfig.getBookings().isEnabled() && oAuthService.isConfigured();
    }

    private WebClient buildWebClient(String accessToken) {
        return webClientBuilder
            .baseUrl(zohoConfig.getBookings().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
            .build();
    }

    private TimeSlot mapToTimeSlot(Map<String, Object> data) {
        TimeSlot slot = new TimeSlot();
        slot.setStartTime(LocalTime.parse((String) data.get("from_time"), TIME_FORMAT));
        slot.setEndTime(LocalTime.parse((String) data.get("to_time"), TIME_FORMAT));
        slot.setAvailable(Boolean.TRUE.equals(data.get("is_available")));
        return slot;
    }

    private AppointmentDetails mapToAppointmentDetails(Map<String, Object> data) {
        AppointmentDetails details = new AppointmentDetails();
        details.setBookingId((String) data.get("booking_id"));
        details.setConfirmationNumber((String) data.get("confirmation_number"));
        details.setServiceName((String) data.get("service_name"));
        details.setStaffName((String) data.get("staff_name"));
        details.setCustomerName((String) data.get("customer_name"));
        details.setCustomerEmail((String) data.get("customer_email"));
        details.setStatus((String) data.get("status"));
        details.setMeetingLink((String) data.get("meeting_link"));
        
        String startTime = (String) data.get("from_time");
        if (startTime != null) {
            details.setStartTime(LocalDateTime.parse(startTime, DATETIME_FORMAT));
        }
        
        String endTime = (String) data.get("to_time");
        if (endTime != null) {
            details.setEndTime(LocalDateTime.parse(endTime, DATETIME_FORMAT));
        }
        
        return details;
    }

    private ServiceInfo mapToServiceInfo(Map<String, Object> data) {
        ServiceInfo info = new ServiceInfo();
        info.setServiceId((String) data.get("service_id"));
        info.setName((String) data.get("name"));
        info.setDescription((String) data.get("description"));
        info.setDurationMinutes(((Number) data.get("duration")).intValue());
        info.setPrice(data.get("price") != null ? ((Number) data.get("price")).doubleValue() : 0.0);
        info.setActive(Boolean.TRUE.equals(data.get("is_active")));
        return info;
    }

    // ==================== DTOs ====================

    @Data
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;
    }

    @Data
    public static class BookingRequest {
        private String serviceId;
        private String staffId;
        private LocalDateTime startTime;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String notes;
        private String crmDealId; // Link to Zoho CRM deal
        private Map<String, String> customFields;
    }

    @Data
    public static class BookingResult {
        private boolean success;
        private boolean skipped;
        private String message;
        private String bookingId;
        private String confirmationNumber;
        private String meetingLink;
        private String calendarLink;

        public static BookingResult success() {
            BookingResult result = new BookingResult();
            result.success = true;
            return result;
        }

        public static BookingResult failed(String error) {
            BookingResult result = new BookingResult();
            result.success = false;
            result.message = error;
            return result;
        }

        public static BookingResult disabled() {
            BookingResult result = new BookingResult();
            result.skipped = true;
            result.message = "Zoho Bookings is disabled";
            return result;
        }
    }

    @Data
    public static class AppointmentDetails {
        private String bookingId;
        private String confirmationNumber;
        private String serviceName;
        private String staffName;
        private String customerName;
        private String customerEmail;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status; // confirmed, cancelled, completed, no-show
        private String meetingLink;
        private String notes;
    }

    @Data
    public static class ServiceInfo {
        private String serviceId;
        private String name;
        private String description;
        private int durationMinutes;
        private double price;
        private boolean active;
    }
}
