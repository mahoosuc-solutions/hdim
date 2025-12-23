package com.healthdata.cache;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CacheEvictionService")
class CacheEvictionServiceTest {

    @Test
    @DisplayName("Should allow tenant cache eviction without errors")
    void shouldEvictTenantCaches() {
        CacheEvictionService service = new CacheEvictionService();

        assertThatCode(() -> service.evictPhiCachesForTenants(Set.of("tenant-1")))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should allow full cache eviction without errors")
    void shouldEvictAllCaches() {
        CacheEvictionService service = new CacheEvictionService();

        assertThatCode(service::evictAllPhiCaches).doesNotThrowAnyException();
    }
}
