package com.healthdata.healthixadapter.mpi;

import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.CrossReference;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchRequest;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchResult;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.PatientIdentifier;
import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("VeratoMpiProxy")
class VeratoMpiProxyTest {

    @Mock
    private RestTemplate mpiRestTemplate;

    @Mock
    private AdapterSpanHelper spanHelper;

    private VeratoMpiProxy proxy;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(spanHelper.traced(anyString(), any(Supplier.class), any(String[].class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        proxy = new VeratoMpiProxy(mpiRestTemplate, registry, spanHelper);
    }

    @Test
    @DisplayName("should post match request and return result")
    void queryPatientMatch_shouldReturnResult() {
        MpiMatchRequest request = MpiMatchRequest.builder()
                .tenantId("tenant-1")
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth("1985-03-15")
                .identifiers(List.of(
                        PatientIdentifier.builder()
                                .system("urn:oid:2.16.840.1.113883.4.1")
                                .value("MRN-12345")
                                .assigningAuthority("GoodHealth Hospital")
                                .build()))
                .build();

        MpiMatchResult expectedResult = MpiMatchResult.builder()
                .enterpriseId("EUID-001")
                .matchConfidence(0.98)
                .matchStatus("MATCH")
                .crossReferences(List.of(
                        CrossReference.builder()
                                .organization("Providence")
                                .patientId("PROV-99887")
                                .build()))
                .build();

        when(mpiRestTemplate.postForObject(
                eq("/api/v1/patient-identity/match"),
                any(MpiMatchRequest.class),
                eq(MpiMatchResult.class)))
                .thenReturn(expectedResult);

        MpiMatchResult result = proxy.queryPatientMatch(request);

        assertThat(result.getEnterpriseId()).isEqualTo("EUID-001");
        assertThat(result.getMatchConfidence()).isEqualTo(0.98);
        assertThat(result.getCrossReferences()).hasSize(1);
        verify(mpiRestTemplate).postForObject(eq("/api/v1/patient-identity/match"), any(), eq(MpiMatchResult.class));
    }

    @Test
    @DisplayName("should propagate exception when MPI is unreachable")
    void queryPatientMatch_shouldPropagateException() {
        MpiMatchRequest request = MpiMatchRequest.builder()
                .tenantId("tenant-1")
                .identifiers(List.of())
                .build();

        when(mpiRestTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> proxy.queryPatientMatch(request))
                .isInstanceOf(ResourceAccessException.class);
    }
}
