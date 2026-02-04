package com.healthdata.sales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.sales.dto.LinkedInCampaignDTO;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
import com.healthdata.sales.exception.DuplicateResourceException;
import com.healthdata.sales.exception.ResourceNotFoundException;
import com.healthdata.sales.service.LinkedInCampaignService;
import com.healthdata.sales.service.LinkedInOutreachService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LinkedInController - focusing on LinkedIn Campaign CRUD operations.
 *
 * Tests all campaign endpoints with MockMvc, verifying:
 * - HTTP status codes
 * - Response content types
 * - JSON response structure
 * - Service method invocations
 * - Header validation (X-Tenant-ID)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LinkedIn Controller Tests")
@Tag("unit")
class LinkedInControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private LinkedInCampaignService campaignService;

    @Mock
    private LinkedInOutreachService outreachService;

    @InjectMocks
    private LinkedInController controller;

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CAMPAIGN_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private LinkedInCampaignDTO testCampaignDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter jsonConverter =
            new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(jsonConverter)
            .build();

        testCampaignDTO = createTestCampaignDTO();
    }

    /**
     * Custom argument resolver for Pageable in standalone MockMvc tests.
     * Spring Data Web support is not available in standalone setup.
     */
    static class PageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Pageable.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            String page = webRequest.getParameter("page");
            String size = webRequest.getParameter("size");
            int pageNum = page != null ? Integer.parseInt(page) : 0;
            int pageSize = size != null ? Integer.parseInt(size) : 20;
            return PageRequest.of(pageNum, pageSize);
        }
    }

    private LinkedInCampaignDTO createTestCampaignDTO() {
        return LinkedInCampaignDTO.builder()
            .id(CAMPAIGN_ID)
            .name("Test Campaign")
            .description("Test description")
            .status(CampaignStatus.DRAFT)
            .targetCriteria("Healthcare executives")
            .dailyLimit(25)
            .totalSent(100)
            .totalAccepted(45)
            .totalReplied(20)
            .acceptanceRate(45.0)
            .createdBy(USER_ID)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ==================== GET /api/sales/linkedin/campaigns Tests ====================

    @Nested
    @DisplayName("GET /api/sales/linkedin/campaigns")
    class ListCampaigns {

        @Test
        @DisplayName("should return paginated campaigns")
        void shouldReturnPaginatedCampaigns() throws Exception {
            // Arrange
            Page<LinkedInCampaignDTO> campaignPage = new PageImpl<>(
                List.of(testCampaignDTO), PageRequest.of(0, 10), 1
            );
            when(campaignService.findAll(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(campaignPage);

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Campaign"))
                .andExpect(jsonPath("$.totalElements").value(1));

            verify(campaignService).findAll(eq(TENANT_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus() throws Exception {
            // Arrange
            testCampaignDTO.setStatus(CampaignStatus.ACTIVE);
            Page<LinkedInCampaignDTO> campaignPage = new PageImpl<>(
                List.of(testCampaignDTO), PageRequest.of(0, 10), 1
            );
            when(campaignService.findByStatus(eq(TENANT_ID), eq(CampaignStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(campaignPage);

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

            verify(campaignService).findByStatus(eq(TENANT_ID), eq(CampaignStatus.ACTIVE), any(Pageable.class));
            verify(campaignService, never()).findAll(any(), any());
        }

        @Test
        @DisplayName("should search by name when provided")
        void shouldSearchByName() throws Exception {
            // Arrange
            Page<LinkedInCampaignDTO> campaignPage = new PageImpl<>(
                List.of(testCampaignDTO), PageRequest.of(0, 10), 1
            );
            when(campaignService.searchByName(eq(TENANT_ID), eq("Test"), any(Pageable.class)))
                .thenReturn(campaignPage);

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .param("search", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Campaign"));

            verify(campaignService).searchByName(eq(TENANT_ID), eq("Test"), any(Pageable.class));
            verify(campaignService, never()).findAll(any(), any());
            verify(campaignService, never()).findByStatus(any(), any(), any());
        }

        @Test
        @DisplayName("should return empty page when no campaigns exist")
        void shouldReturnEmptyPage() throws Exception {
            // Arrange
            Page<LinkedInCampaignDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(campaignService.findAll(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // ==================== GET /api/sales/linkedin/campaigns/{id} Tests ====================

    @Nested
    @DisplayName("GET /api/sales/linkedin/campaigns/{id}")
    class GetCampaign {

        @Test
        @DisplayName("should return campaign when found")
        void shouldReturnCampaignWhenFound() throws Exception {
            // Arrange
            when(campaignService.findById(TENANT_ID, CAMPAIGN_ID))
                .thenReturn(Optional.of(testCampaignDTO));

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns/{id}", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(CAMPAIGN_ID.toString()))
                .andExpect(jsonPath("$.name").value("Test Campaign"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.dailyLimit").value(25))
                .andExpect(jsonPath("$.totalSent").value(100))
                .andExpect(jsonPath("$.totalAccepted").value(45))
                .andExpect(jsonPath("$.acceptanceRate").value(45.0));

            verify(campaignService).findById(TENANT_ID, CAMPAIGN_ID);
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignService.findById(TENANT_ID, nonExistentId))
                .thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns/{id}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ==================== POST /api/sales/linkedin/campaigns Tests ====================

    @Nested
    @DisplayName("POST /api/sales/linkedin/campaigns")
    class CreateCampaign {

        @Test
        @DisplayName("should create campaign and return 200")
        void shouldCreateCampaign() throws Exception {
            // Arrange
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("New Campaign")
                .description("New description")
                .targetCriteria("Tech executives")
                .dailyLimit(30)
                .build();

            LinkedInCampaignDTO savedDTO = LinkedInCampaignDTO.builder()
                .id(UUID.randomUUID())
                .name("New Campaign")
                .description("New description")
                .status(CampaignStatus.DRAFT)
                .targetCriteria("Tech executives")
                .dailyLimit(30)
                .totalSent(0)
                .totalAccepted(0)
                .totalReplied(0)
                .acceptanceRate(0.0)
                .createdBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();

            when(campaignService.create(eq(TENANT_ID), any(LinkedInCampaignDTO.class), eq(USER_ID)))
                .thenReturn(savedDTO);

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .header("X-User-ID", USER_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("New Campaign"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

            verify(campaignService).create(eq(TENANT_ID), any(LinkedInCampaignDTO.class), eq(USER_ID));
        }

        @Test
        @DisplayName("should return 409 when campaign name already exists")
        void shouldReturn409ForDuplicateName() throws Exception {
            // Arrange
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("Existing Campaign")
                .build();

            when(campaignService.create(eq(TENANT_ID), any(LinkedInCampaignDTO.class), any()))
                .thenThrow(new DuplicateResourceException("Campaign with name 'Existing Campaign' already exists"));

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            // Arrange
            LinkedInCampaignDTO inputDTO = LinkedInCampaignDTO.builder()
                .name("")  // Blank name
                .build();

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());

            verify(campaignService, never()).create(any(), any(), any());
        }
    }

    // ==================== PUT /api/sales/linkedin/campaigns/{id} Tests ====================

    @Nested
    @DisplayName("PUT /api/sales/linkedin/campaigns/{id}")
    class UpdateCampaign {

        @Test
        @DisplayName("should update campaign and return 200")
        void shouldUpdateCampaign() throws Exception {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Updated Campaign")
                .description("Updated description")
                .dailyLimit(50)
                .build();

            LinkedInCampaignDTO updatedDTO = LinkedInCampaignDTO.builder()
                .id(CAMPAIGN_ID)
                .name("Updated Campaign")
                .description("Updated description")
                .status(CampaignStatus.DRAFT)
                .dailyLimit(50)
                .build();

            when(campaignService.update(eq(TENANT_ID), eq(CAMPAIGN_ID), any(LinkedInCampaignDTO.class)))
                .thenReturn(updatedDTO);

            // Act & Assert
            mockMvc.perform(put("/api/sales/linkedin/campaigns/{id}", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Campaign"))
                .andExpect(jsonPath("$.description").value("Updated description"));

            verify(campaignService).update(eq(TENANT_ID), eq(CAMPAIGN_ID), any(LinkedInCampaignDTO.class));
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Updated Campaign")
                .build();

            when(campaignService.update(eq(TENANT_ID), eq(nonExistentId), any(LinkedInCampaignDTO.class)))
                .thenThrow(new ResourceNotFoundException("Campaign not found: " + nonExistentId));

            // Act & Assert
            mockMvc.perform(put("/api/sales/linkedin/campaigns/{id}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when changing to existing name")
        void shouldReturn409WhenChangingToExistingName() throws Exception {
            // Arrange
            LinkedInCampaignDTO updateDTO = LinkedInCampaignDTO.builder()
                .name("Existing Name")
                .build();

            when(campaignService.update(eq(TENANT_ID), eq(CAMPAIGN_ID), any(LinkedInCampaignDTO.class)))
                .thenThrow(new DuplicateResourceException("Campaign with name 'Existing Name' already exists"));

            // Act & Assert
            mockMvc.perform(put("/api/sales/linkedin/campaigns/{id}", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict());
        }
    }

    // ==================== DELETE /api/sales/linkedin/campaigns/{id} Tests ====================

    @Nested
    @DisplayName("DELETE /api/sales/linkedin/campaigns/{id}")
    class DeleteCampaign {

        @Test
        @DisplayName("should delete campaign and return 204")
        void shouldDeleteCampaign() throws Exception {
            // Arrange
            doNothing().when(campaignService).delete(TENANT_ID, CAMPAIGN_ID);

            // Act & Assert
            mockMvc.perform(delete("/api/sales/linkedin/campaigns/{id}", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isNoContent());

            verify(campaignService).delete(TENANT_ID, CAMPAIGN_ID);
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Campaign not found: " + nonExistentId))
                .when(campaignService).delete(TENANT_ID, nonExistentId);

            // Act & Assert
            mockMvc.perform(delete("/api/sales/linkedin/campaigns/{id}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ==================== POST /api/sales/linkedin/campaigns/{id}/activate Tests ====================

    @Nested
    @DisplayName("POST /api/sales/linkedin/campaigns/{id}/activate")
    class ActivateCampaign {

        @Test
        @DisplayName("should activate campaign and return 200")
        void shouldActivateCampaign() throws Exception {
            // Arrange
            LinkedInCampaignDTO activatedDTO = LinkedInCampaignDTO.builder()
                .id(CAMPAIGN_ID)
                .name("Test Campaign")
                .status(CampaignStatus.ACTIVE)
                .build();

            when(campaignService.activate(TENANT_ID, CAMPAIGN_ID))
                .thenReturn(activatedDTO);

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns/{id}/activate", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(campaignService).activate(TENANT_ID, CAMPAIGN_ID);
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignService.activate(TENANT_ID, nonExistentId))
                .thenThrow(new ResourceNotFoundException("Campaign not found: " + nonExistentId));

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns/{id}/activate", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ==================== POST /api/sales/linkedin/campaigns/{id}/pause Tests ====================

    @Nested
    @DisplayName("POST /api/sales/linkedin/campaigns/{id}/pause")
    class PauseCampaign {

        @Test
        @DisplayName("should pause campaign and return 200")
        void shouldPauseCampaign() throws Exception {
            // Arrange
            LinkedInCampaignDTO pausedDTO = LinkedInCampaignDTO.builder()
                .id(CAMPAIGN_ID)
                .name("Test Campaign")
                .status(CampaignStatus.PAUSED)
                .build();

            when(campaignService.pause(TENANT_ID, CAMPAIGN_ID))
                .thenReturn(pausedDTO);

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns/{id}/pause", CAMPAIGN_ID)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

            verify(campaignService).pause(TENANT_ID, CAMPAIGN_ID);
        }

        @Test
        @DisplayName("should return 404 when campaign not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(campaignService.pause(TENANT_ID, nonExistentId))
                .thenThrow(new ResourceNotFoundException("Campaign not found: " + nonExistentId));

            // Act & Assert
            mockMvc.perform(post("/api/sales/linkedin/campaigns/{id}/pause", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID.toString()))
                .andExpect(status().isNotFound());
        }
    }

    // ==================== Multi-Tenant Isolation Tests ====================

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolation {

        @Test
        @DisplayName("should use correct tenant ID for all operations")
        void shouldUseTenantIdFromHeader() throws Exception {
            // Arrange
            UUID specificTenantId = UUID.fromString("99999999-9999-9999-9999-999999999999");
            Page<LinkedInCampaignDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(campaignService.findAll(eq(specificTenantId), any(Pageable.class)))
                .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/sales/linkedin/campaigns")
                    .header("X-Tenant-ID", specificTenantId.toString()))
                .andExpect(status().isOk());

            verify(campaignService).findAll(eq(specificTenantId), any(Pageable.class));
        }
    }

    /**
     * Simple exception handler for test purposes.
     * In production, this would be more comprehensive.
     */
    @org.springframework.web.bind.annotation.ControllerAdvice
    static class GlobalExceptionHandler {

        @org.springframework.web.bind.annotation.ExceptionHandler(ResourceNotFoundException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
        @org.springframework.web.bind.annotation.ResponseBody
        public java.util.Map<String, String> handleNotFound(ResourceNotFoundException ex) {
            return java.util.Map.of("error", ex.getMessage());
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(DuplicateResourceException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CONFLICT)
        @org.springframework.web.bind.annotation.ResponseBody
        public java.util.Map<String, String> handleDuplicate(DuplicateResourceException ex) {
            return java.util.Map.of("error", ex.getMessage());
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(
            org.springframework.web.bind.MethodArgumentNotValidException.class)
        @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
        @org.springframework.web.bind.annotation.ResponseBody
        public java.util.Map<String, String> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
            return java.util.Map.of("error", "Validation failed");
        }
    }
}
