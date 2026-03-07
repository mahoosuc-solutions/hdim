package com.healthdata.ihegateway.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IheGatewayHealthControllerTest {

    private final IheGatewayHealthController controller = new IheGatewayHealthController();

    @Test
    void health_returnsUpStatus() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "ihe-gateway-service");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
