package com.healthdata.database.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrafficTier Tests")
class TrafficTierTest {

    @Test
    @DisplayName("HIGH tier provides 50 connections and 10 min idle")
    void highTier_provides50ConnectionsAnd10MinIdle() {
        assertThat(TrafficTier.HIGH.getPoolSize()).isEqualTo(50);
        assertThat(TrafficTier.HIGH.getMinIdle()).isEqualTo(10);
    }

    @Test
    @DisplayName("MEDIUM tier provides 20 connections and 5 min idle")
    void mediumTier_provides20ConnectionsAnd5MinIdle() {
        assertThat(TrafficTier.MEDIUM.getPoolSize()).isEqualTo(20);
        assertThat(TrafficTier.MEDIUM.getMinIdle()).isEqualTo(5);
    }

    @Test
    @DisplayName("LOW tier provides 10 connections and 5 min idle")
    void lowTier_provides10ConnectionsAnd5MinIdle() {
        assertThat(TrafficTier.LOW.getPoolSize()).isEqualTo(10);
        assertThat(TrafficTier.LOW.getMinIdle()).isEqualTo(5);
    }
}
