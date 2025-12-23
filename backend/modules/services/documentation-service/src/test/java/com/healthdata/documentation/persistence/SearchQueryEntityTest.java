package com.healthdata.documentation.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Search Query Entity Tests")
class SearchQueryEntityTest {

    @Test
    @DisplayName("Should set searchedAt on create when missing")
    void shouldSetSearchedAtOnCreate() {
        SearchQueryEntity entity = new SearchQueryEntity();

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getSearchedAt()).isNotNull();
    }
}
