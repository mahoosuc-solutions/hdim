package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HealthScoreService Inner Class Tests")
class HealthScoreServiceInnerClassTest {

    @Test
    @DisplayName("Should expose condition data via getters")
    void shouldExposeConditionDataViaGetters() throws Exception {
        Class<?> conditionTypeClass =
            Class.forName("com.healthdata.quality.service.HealthScoreService$ConditionType");
        @SuppressWarnings("unchecked")
        Class<? extends Enum> enumClass = (Class<? extends Enum>) conditionTypeClass;
        Enum<?> conditionType = Enum.valueOf(enumClass, "DIABETES_TYPE_2");

        Class<?> conditionDataClass =
            Class.forName("com.healthdata.quality.service.HealthScoreService$ConditionData");
        Constructor<?> ctor = conditionDataClass.getDeclaredConstructor(
            conditionTypeClass, String.class, String.class, String.class, boolean.class);
        ctor.setAccessible(true);

        Object conditionData = ctor.newInstance(
            conditionType,
            "code-1",
            "Diabetes Type 2",
            "sev-1",
            true
        );

        assertThat(conditionDataClass.getMethod("getConditionType").invoke(conditionData))
            .isEqualTo(conditionType);
        assertThat(conditionDataClass.getMethod("getConditionCode").invoke(conditionData))
            .isEqualTo("code-1");
        assertThat(conditionDataClass.getMethod("getDisplay").invoke(conditionData))
            .isEqualTo("Diabetes Type 2");
        assertThat(conditionDataClass.getMethod("getSeverityCode").invoke(conditionData))
            .isEqualTo("sev-1");
        assertThat(conditionDataClass.getMethod("isActive").invoke(conditionData))
            .isEqualTo(true);
    }
}
