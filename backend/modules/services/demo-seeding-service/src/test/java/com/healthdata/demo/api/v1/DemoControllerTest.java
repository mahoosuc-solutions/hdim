package com.healthdata.demo.api.v1;

import com.healthdata.demo.application.DemoProgressService;
import com.healthdata.demo.application.DemoResetService;
import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.ScenarioLoaderService;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoController.class)
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
}
