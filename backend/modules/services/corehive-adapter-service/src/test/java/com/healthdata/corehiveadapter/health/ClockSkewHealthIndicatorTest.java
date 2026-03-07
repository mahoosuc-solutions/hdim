package com.healthdata.corehiveadapter.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ClockSkewHealthIndicatorTest {

    @Test
    void health_whenClockInSync_returnsUp() {
        var indicator = new ClockSkewHealthIndicator(1000L);
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("maxAllowedSkewMs");
        assertThat(health.getDetails()).containsKey("iheCtCompliant");
    }

    @Test
    void health_detailsIncludeTimestamp() {
        var indicator = new ClockSkewHealthIndicator(1000L);
        Health health = indicator.health();

        assertThat(health.getDetails()).containsKey("serverTimeUtc");
    }
}
