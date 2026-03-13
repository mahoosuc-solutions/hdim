package com.healthdata.caregap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.repository.RefreshTokenRepository;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.repository.UserRepository;
import com.healthdata.caregap.api.v1.dto.DetectGapRequest;
import com.healthdata.caregap.persistence.StarRatingProjectionRepository;
import com.healthdata.caregap.projection.StarRatingProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Tag("integration")
@DisplayName("Stars Projection Integration Test")
class StarsProjectionIntegrationTest {

    private static final String TENANT_ID = "TENANT-STARS";
    private static final String API_BASE_PATH = "/api/v1/gaps/events";

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("apache/kafka:3.8.0")
    )
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
        .waitingFor(Wait.forListeningPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StarRatingProjectionRepository starRatingProjectionRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TenantRepository tenantRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        starRatingProjectionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should update persisted stars projection after detect and close lifecycle events")
    void shouldUpdatePersistedStarsProjectionAfterDetectAndCloseLifecycleEvents() throws Exception {
        DetectGapRequest request = new DetectGapRequest(
            "PATIENT-STARS", "COL", "Missing colorectal screening", "HIGH"
        );

        mockMvc.perform(post(API_BASE_PATH + "/detect")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        StarRatingProjection detectedProjection = awaitStarsProjection(
            projection -> projection.getOpenGapCount() == 1 && projection.getClosedGapCount() == 0
        );
        assertThat(detectedProjection.getLastTriggerEvent()).isEqualTo("gap.detected:COL");

        String closeRequest = "{" +
            "\"patientId\": \"PATIENT-STARS\"," +
            "\"gapCode\": \"COL\"," +
            "\"reason\": \"Screening completed\"" +
            "}";

        mockMvc.perform(post(API_BASE_PATH + "/close")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closeRequest))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("CLOSED"));

        StarRatingProjection closedProjection = awaitStarsProjection(
            projection -> projection.getOpenGapCount() == 0 && projection.getClosedGapCount() == 1
        );
        assertThat(closedProjection.getLastTriggerEvent()).isEqualTo("gap.closed:COL");
    }

    private StarRatingProjection awaitStarsProjection(Predicate<StarRatingProjection> predicate) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();

        while (System.nanoTime() < deadline) {
            var projection = starRatingProjectionRepository.findById(TENANT_ID);
            if (projection.isPresent() && predicate.test(projection.get())) {
                return projection.get();
            }
            Thread.sleep(100);
        }

        fail("Timed out waiting for persisted Stars projection for tenant " + TENANT_ID);
        return null;
    }
}
