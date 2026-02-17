package com.healthdata.demo.api.v1;

import com.healthdata.demo.application.DemoProgressService;
import com.healthdata.demo.application.DemoResetService;
import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.ScenarioLoaderService;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(DemoController.class)
@ContextConfiguration(classes = DemoControllerTest.TestConfig.class)
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoSeedingService seedingService;

    @MockBean
    private DemoResetService resetService;

    @MockBean
    private DemoProgressService progressService;

    @MockBean
    private ScenarioLoaderService scenarioLoaderService;

    @MockBean
    private DemoSessionRepository sessionRepository;

    @MockBean(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
    })
    @Import(DemoController.class)
    static class TestConfig {
    }

    @Test
    void loadScenario_AllowsOverridePayload() throws Exception {
        ScenarioLoaderService.LoadResult result = new ScenarioLoaderService.LoadResult();
        result.setScenarioName("multi-tenant");
        result.setSuccess(true);
        result.setPatientCount(300);
        result.setCareGapCount(90);
        when(scenarioLoaderService.loadScenario(eq("multi-tenant"), eq(100), eq(30)))
            .thenReturn(result);

        String payload = "{\"patientsPerTenant\":100,\"careGapPercentage\":30}";
        mockMvc.perform(post("/api/v1/demo/scenarios/multi-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());
    }

    @Test
    void loadScenario_RejectsInvalidOverridePayload() throws Exception {
        String payload = "{\"patientsPerTenant\":0,\"careGapPercentage\":101}";
        mockMvc.perform(post("/api/v1/demo/scenarios/multi-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void streamCurrentProgress_ReturnsNotFound_WhenNoCurrentSession() throws Exception {
        when(sessionRepository.findCurrentSession()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/demo/sessions/current/progress/stream"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testWebhook_EchoesPayloadAndHeaders() throws Exception {
        String payload = "{\"integration\":\"sandbox\",\"status\":\"ok\"}";

        mockMvc.perform(post("/api/v1/demo/webhooks/test")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "sandbox-tenant")
                .header("X-Webhook-Event", "integration.test")
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.tenantId").value("sandbox-tenant"))
            .andExpect(jsonPath("$.eventType").value("integration.test"))
            .andExpect(jsonPath("$.payload.integration").value("sandbox"))
            .andExpect(jsonPath("$.payload.status").value("ok"));
    }
}
