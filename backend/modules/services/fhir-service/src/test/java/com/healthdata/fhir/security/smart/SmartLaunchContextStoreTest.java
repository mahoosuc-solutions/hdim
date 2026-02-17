package com.healthdata.fhir.security.smart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SMART Launch Context Store Tests")
class SmartLaunchContextStoreTest {

    @Test
    @DisplayName("Should store and resolve launch context by opaque launch ID")
    void shouldStoreAndResolveLaunchContext() {
        SmartLaunchContextStore store = new SmartLaunchContextStore(60);

        String launchId = store.storeLaunchContext(Map.of("patient", "patient-1"));

        assertThat(launchId).startsWith("lc_");
        assertThat(store.resolveLaunchContext(launchId)).isPresent();
        assertThat(store.resolveLaunchContext(launchId).orElseThrow().get("patient")).isEqualTo("patient-1");
    }

    @Test
    @DisplayName("Should expire launch context after TTL")
    void shouldExpireLaunchContextAfterTtl() throws Exception {
        SmartLaunchContextStore store = new SmartLaunchContextStore(1);
        String launchId = store.storeLaunchContext(Map.of("patient", "patient-2"));

        Thread.sleep(1200);

        assertThat(store.resolveLaunchContext(launchId)).isEmpty();
    }
}
