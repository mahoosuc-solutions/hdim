package com.healthdata.qrda.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.qrda.dto.QrdaExportRequest;
import com.healthdata.qrda.persistence.QrdaExportJobEntity;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobStatus;
import com.healthdata.qrda.persistence.QrdaExportJobEntity.QrdaJobType;
import com.healthdata.qrda.persistence.QrdaExportJobRepository;
import com.healthdata.qrda.service.QrdaCategoryIService;
import com.healthdata.qrda.service.QrdaCategoryIIIService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for QrdaExportController.
 * Tests REST API endpoints for QRDA export operations using standalone MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class QrdaExportControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private QrdaCategoryIService categoryIService;

    @Mock
    private QrdaCategoryIIIService categoryIIIService;

    @Mock
    private QrdaExportJobRepository jobRepository;

    @InjectMocks
    private QrdaExportController controller;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/qrda";

    private UserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .username("testuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        // Custom argument resolver for @AuthenticationPrincipal
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class) ||
                       UserDetails.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        // Custom argument resolver for Pageable
        HandlerMethodArgumentResolver pageableResolver = new HandlerMethodArgumentResolver() {
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
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(authPrincipalResolver, pageableResolver)
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("POST /category-i/generate")
    class GenerateCategoryITests {

        @Test
        @DisplayName("Should initiate Category I export successfully")
        void generateCategoryI_validRequest_returns202() throws Exception {
            // Arrange
            QrdaExportRequest request = createValidExportRequest(QrdaJobType.QRDA_I);
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_I, QrdaJobStatus.PENDING);

            when(categoryIService.initiateExport(eq(TENANT_ID), any(QrdaExportRequest.class), any()))
                .thenReturn(job);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/category-i/generate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .principal(() -> "testuser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobType").value("QRDA_I"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("POST /category-iii/generate")
    class GenerateCategoryIIITests {

        @Test
        @DisplayName("Should initiate Category III export successfully")
        void generateCategoryIII_validRequest_returns202() throws Exception {
            // Arrange
            QrdaExportRequest request = createValidExportRequest(QrdaJobType.QRDA_III);
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.PENDING);

            when(categoryIIIService.initiateExport(eq(TENANT_ID), any(QrdaExportRequest.class), any()))
                .thenReturn(job);

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/category-iii/generate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .principal(() -> "testuser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.jobType").value("QRDA_III"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("GET /jobs/{jobId}")
    class GetJobStatusTests {

        @Test
        @DisplayName("Should return job status when found")
        void getJobStatus_jobExists_returnsJob() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 404 when job not found")
        void getJobStatus_jobNotFound_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}", UUID.randomUUID())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should not return job from different tenant")
        void getJobStatus_differentTenant_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(JOB_ID, "other-tenant")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}", JOB_ID)
                    .header("X-Tenant-ID", "other-tenant"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /jobs")
    class ListJobsTests {

        @Test
        @DisplayName("Should call repository with correct tenant")
        void listJobs_callsRepositoryWithCorrectTenant() throws Exception {
            // Arrange
            when(jobRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

            // Act
            mockMvc.perform(get(BASE_URL + "/jobs")
                    .header("X-Tenant-ID", TENANT_ID));

            // Assert - verify correct tenant ID was passed
            verify(jobRepository).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("Should call repository with different tenant IDs")
        void listJobs_respectsTenantIsolation() throws Exception {
            // Arrange
            String otherTenant = "other-tenant";
            when(jobRepository.findByTenantIdOrderByCreatedAtDesc(eq(otherTenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

            // Act
            mockMvc.perform(get(BASE_URL + "/jobs")
                    .header("X-Tenant-ID", otherTenant));

            // Assert - verify correct tenant ID was passed (not default tenant)
            verify(jobRepository).findByTenantIdOrderByCreatedAtDesc(eq(otherTenant), any(Pageable.class));
            verify(jobRepository, never()).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /jobs/{jobId}/download")
    class DownloadQrdaTests {

        @Test
        @DisplayName("Should return 404 when job not found")
        void downloadQrda_jobNotFound_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}/download", UUID.randomUUID())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when job not completed")
        void downloadQrda_jobNotCompleted_returns400() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}/download", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when document location is null")
        void downloadQrda_noDocumentLocation_returns404() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.COMPLETED);
            job.setDocumentLocation(null);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(get(BASE_URL + "/jobs/{jobId}/download", JOB_ID)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /jobs/{jobId}/cancel")
    class CancelJobTests {

        @Test
        @DisplayName("Should cancel pending job")
        void cancelJob_pendingJob_cancelsSuccessfully() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.PENDING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity saved = inv.getArgument(0);
                return saved;
            });

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/jobs/{jobId}/cancel", JOB_ID)
                    .principal(() -> "testuser")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should cancel running job")
        void cancelJob_runningJob_cancelsSuccessfully() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(QrdaExportJobEntity.class))).thenAnswer(inv -> {
                QrdaExportJobEntity saved = inv.getArgument(0);
                return saved;
            });

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/jobs/{jobId}/cancel", JOB_ID)
                    .principal(() -> "testuser")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("Should return 400 for completed job")
        void cancelJob_completedJob_returns400() throws Exception {
            // Arrange
            QrdaExportJobEntity job = createJobEntity(QrdaJobType.QRDA_III, QrdaJobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID)).thenReturn(Optional.of(job));

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/jobs/{jobId}/cancel", JOB_ID)
                    .principal(() -> "testuser")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 for non-existent job")
        void cancelJob_jobNotFound_returns404() throws Exception {
            // Arrange
            when(jobRepository.findByIdAndTenantId(any(UUID.class), anyString())).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post(BASE_URL + "/jobs/{jobId}/cancel", UUID.randomUUID())
                    .principal(() -> "testuser")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    // Helper methods

    private QrdaExportRequest createValidExportRequest(QrdaJobType jobType) {
        return QrdaExportRequest.builder()
            .jobType(jobType)
            .measureIds(List.of("CMS125v12", "CMS130v11"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .validateDocuments(true)
            .includeSupplementalData(true)
            .build();
    }

    private QrdaExportJobEntity createJobEntity(QrdaJobType jobType, QrdaJobStatus status) {
        return QrdaExportJobEntity.builder()
            .id(JOB_ID)
            .tenantId(TENANT_ID)
            .jobType(jobType)
            .status(status)
            .measureIds(List.of("CMS125v12"))
            .periodStart(LocalDate.of(2024, 1, 1))
            .periodEnd(LocalDate.of(2024, 12, 31))
            .requestedBy("testuser")
            .createdAt(LocalDateTime.now())
            .documentLocation("/tmp/qrda/test.xml")
            .build();
    }
}
