package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HealthScoreService VitalSignData Tests")
class HealthScoreServiceVitalSignDataTest {

    @Test
    @DisplayName("Should expose VitalSignData fields via getters")
    void shouldExposeVitalSignDataFields() throws Exception {
        Class<?> typeClass = Class.forName("com.healthdata.quality.service.HealthScoreService$VitalSignType");
        Object type = typeClass.getEnumConstants()[0];

        Class<?> dataClass = Class.forName("com.healthdata.quality.service.HealthScoreService$VitalSignData");
        Constructor<?> constructor = dataClass.getDeclaredConstructor(
            typeClass, String.class, String.class, Double.class, String.class);
        constructor.setAccessible(true);

        Object data = constructor.newInstance(type, "1234-5", "Blood Pressure", 120.5, "mmHg");

        Method getType = dataClass.getDeclaredMethod("getType");
        Method getLoincCode = dataClass.getDeclaredMethod("getLoincCode");
        Method getDisplay = dataClass.getDeclaredMethod("getDisplay");
        Method getValue = dataClass.getDeclaredMethod("getValue");
        Method getUnit = dataClass.getDeclaredMethod("getUnit");

        assertThat(getType.invoke(data)).isEqualTo(type);
        assertThat(getLoincCode.invoke(data)).isEqualTo("1234-5");
        assertThat(getDisplay.invoke(data)).isEqualTo("Blood Pressure");
        assertThat(getValue.invoke(data)).isEqualTo(120.5);
        assertThat(getUnit.invoke(data)).isEqualTo("mmHg");
    }
}
