package com.healthdata.predictive.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskTierTest {

    @Test
    void shouldExposeTierMetadata() {
        assertThat(RiskTier.HIGH.getMinScore()).isEqualTo(50);
        assertThat(RiskTier.HIGH.getMaxScore()).isEqualTo(75);
        assertThat(RiskTier.HIGH.getDescription()).contains("High risk");
    }

    @Test
    void shouldResolveTierFromScoreBoundaries() {
        assertThat(RiskTier.fromScore(0)).isEqualTo(RiskTier.LOW);
        assertThat(RiskTier.fromScore(25)).isEqualTo(RiskTier.MODERATE);
        assertThat(RiskTier.fromScore(50)).isEqualTo(RiskTier.HIGH);
        assertThat(RiskTier.fromScore(75)).isEqualTo(RiskTier.VERY_HIGH);
        assertThat(RiskTier.fromScore(100)).isEqualTo(RiskTier.VERY_HIGH);
    }

    @Test
    void shouldRejectOutOfRangeScores() {
        assertThatThrownBy(() -> RiskTier.fromScore(-1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RiskTier.fromScore(101))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
