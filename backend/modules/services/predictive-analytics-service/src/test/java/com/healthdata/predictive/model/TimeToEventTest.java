package com.healthdata.predictive.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeToEventTest {

    @Test
    void shouldCalculateMonthsFromDays() {
        TimeToEvent event = TimeToEvent.builder()
            .predictedDays(90)
            .build();

        assertThat(event.calculatePredictedMonths()).isEqualTo(3);
    }

    @Test
    void shouldReturnNullWhenDaysMissing() {
        TimeToEvent event = new TimeToEvent();

        assertThat(event.calculatePredictedMonths()).isNull();
    }
}
