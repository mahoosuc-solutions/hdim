package com.healthdata.database.properties;

import com.healthdata.database.config.TrafficTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HikariProperties Tests")
class HikariPropertiesTest {

    @Test
    @DisplayName("Traffic tier HIGH provides effective pool size of 50")
    void trafficTierHigh_providesEffectivePoolSize50() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.HIGH);

        assertThat(props.getEffectiveMaximumPoolSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Traffic tier MEDIUM provides effective pool size of 20")
    void trafficTierMedium_providesEffectivePoolSize20() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.MEDIUM);

        assertThat(props.getEffectiveMaximumPoolSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Traffic tier LOW provides effective pool size of 10")
    void trafficTierLow_providesEffectivePoolSize10() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.LOW);

        assertThat(props.getEffectiveMaximumPoolSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Explicit pool size overrides traffic tier default")
    void explicitPoolSize_overridesTierDefault() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.MEDIUM);
        props.setMaximumPoolSize(25);

        assertThat(props.getEffectiveMaximumPoolSize()).isEqualTo(25);
    }

    @Test
    @DisplayName("No tier or explicit pool size throws IllegalStateException")
    void noTierOrExplicitSize_throwsIllegalStateException() {
        HikariProperties props = new HikariProperties();

        assertThatThrownBy(props::getEffectiveMaximumPoolSize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Either trafficTier or maximumPoolSize must be configured");
    }

    @Test
    @DisplayName("Traffic tier HIGH provides effective min idle of 10")
    void trafficTierHigh_providesEffectiveMinIdle10() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.HIGH);

        assertThat(props.getEffectiveMinimumIdle()).isEqualTo(10);
    }

    @Test
    @DisplayName("Traffic tier MEDIUM provides effective min idle of 5")
    void trafficTierMedium_providesEffectiveMinIdle5() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.MEDIUM);

        assertThat(props.getEffectiveMinimumIdle()).isEqualTo(5);
    }

    @Test
    @DisplayName("Explicit min idle overrides traffic tier default")
    void explicitMinIdle_overridesTierDefault() {
        HikariProperties props = new HikariProperties();
        props.setTrafficTier(TrafficTier.MEDIUM);
        props.setMinimumIdle(8);

        assertThat(props.getEffectiveMinimumIdle()).isEqualTo(8);
    }

    @Test
    @DisplayName("No tier or explicit min idle calculates 20% of pool size")
    void noTierOrExplicitMinIdle_calculates20PercentOfPoolSize() {
        HikariProperties props = new HikariProperties();
        props.setMaximumPoolSize(30);

        assertThat(props.getEffectiveMinimumIdle()).isEqualTo(6); // 20% of 30
    }

    @Test
    @DisplayName("Default connection timeout is 20 seconds")
    void defaultConnectionTimeout_is20Seconds() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getConnectionTimeout()).isEqualTo(20000);
    }

    @Test
    @DisplayName("Default idle timeout is 5 minutes")
    void defaultIdleTimeout_is5Minutes() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getIdleTimeout()).isEqualTo(300000);
    }

    @Test
    @DisplayName("Default max lifetime is 30 minutes (6x idle timeout)")
    void defaultMaxLifetime_is30Minutes() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getMaxLifetime()).isEqualTo(1800000);
        // Verify 6x safety margin
        assertThat(props.getMaxLifetime()).isEqualTo(props.getIdleTimeout() * 6);
    }

    @Test
    @DisplayName("Default keepalive time is 4 minutes (less than idle timeout)")
    void defaultKeepaliveTime_is4Minutes() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getKeepaliveTime()).isEqualTo(240000);
        assertThat(props.getKeepaliveTime()).isLessThan(props.getIdleTimeout());
    }

    @Test
    @DisplayName("Default leak detection threshold is 60 seconds")
    void defaultLeakDetectionThreshold_is60Seconds() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getLeakDetectionThreshold()).isEqualTo(60000);
    }

    @Test
    @DisplayName("Default validation timeout is 5 seconds")
    void defaultValidationTimeout_is5Seconds() {
        HikariProperties props = new HikariProperties();

        assertThat(props.getValidationTimeout()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Default auto-commit is true")
    void defaultAutoCommit_isTrue() {
        HikariProperties props = new HikariProperties();

        assertThat(props.isAutoCommit()).isTrue();
    }
}
