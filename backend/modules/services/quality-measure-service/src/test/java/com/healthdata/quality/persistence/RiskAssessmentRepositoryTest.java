package com.healthdata.quality.persistence;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RiskAssessmentRepositoryTest {

    @Test
    void shouldDelegateDefaultLookupToFindMostRecent() {
        RiskAssessmentRepository repository = mock(RiskAssessmentRepository.class, CALLS_REAL_METHODS);
        UUID patientId = UUID.randomUUID();
        RiskAssessmentEntity entity = new RiskAssessmentEntity();

        when(repository.findMostRecent("tenant-1", patientId)).thenReturn(Optional.of(entity));

        Optional<RiskAssessmentEntity> result =
            repository.findLatestByTenantIdAndPatientId("tenant-1", patientId);

        assertThat(result).contains(entity);
        verify(repository).findMostRecent("tenant-1", patientId);
    }
}
