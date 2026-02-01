package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.quality.measure.MeasureRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

@DisplayName("Population Calculation Service Tests")
class PopulationCalculationServiceTest {

    @Test
    @DisplayName("Should calculate measures for a small population and complete job")
    void shouldCalculateMeasuresAndCompleteJob() throws Exception {
        MeasureCalculationService measureCalculationService = mock(MeasureCalculationService.class);
        MeasureRegistry measureRegistry = mock(MeasureRegistry.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("measure-calc");
        RateLimiter rateLimiter = mock(RateLimiter.class);
        Executor executor = Runnable::run;

        when(rateLimiter.acquirePermission()).thenReturn(true);
        when(measureRegistry.getMeasureIds()).thenReturn(List.of("measure-1"));

        UUID patientId = UUID.randomUUID();
        Map<String, Object> bundle = Map.of(
            "entry", List.of(Map.of("resource", Map.of("id", patientId.toString())))
        );
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(bundle);

        com.healthdata.quality.persistence.JobExecutionRepository jobRepo =
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(any())).thenReturn(java.util.Optional.empty());

        PopulationCalculationService service = new PopulationCalculationService(
            measureCalculationService,
            measureRegistry,
            restTemplate,
            kafkaTemplate,
            circuitBreaker,
            rateLimiter,
            executor,
            jobRepo
        );

        Map<String, Object> response = service.startPopulationCalculation(
            "tenant-1",
            "http://fhir.test",
            "user-1",
            null,
            null
        );
        String jobId = (String) response.get("jobId");
        Thread.sleep(5); // Wait for async execution

        PopulationCalculationService.BatchCalculationJob job = service.getJobStatus(jobId);
        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.COMPLETED);
        assertThat(job.getCompletedCalculations()).isEqualTo(1);
        assertThat(job.getSuccessfulCalculations()).isEqualTo(1);
        assertThat(job.getFailedCalculations()).isEqualTo(0);

        verify(measureCalculationService).calculateMeasure(
            "tenant-1",
            patientId,
            "measure-1",
            "user-1"
        );
        verify(kafkaTemplate, org.mockito.Mockito.atLeastOnce())
            .send(eq("population-calculation-progress"), eq(jobId), anyString());
    }

    @Test
    @DisplayName("Should mark job failed when patient fetch fails")
    void shouldMarkJobFailedWhenFetchFails() throws Exception {
        MeasureCalculationService measureCalculationService = mock(MeasureCalculationService.class);
        MeasureRegistry measureRegistry = mock(MeasureRegistry.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("measure-calc");
        RateLimiter rateLimiter = mock(RateLimiter.class);
        Executor executor = Runnable::run;

        when(rateLimiter.acquirePermission()).thenReturn(true);
        when(measureRegistry.getMeasureIds()).thenReturn(List.of("measure-1"));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("boom"));

        com.healthdata.quality.persistence.JobExecutionRepository jobRepo =
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(any())).thenReturn(java.util.Optional.empty());

        PopulationCalculationService service = new PopulationCalculationService(
            measureCalculationService,
            measureRegistry,
            restTemplate,
            kafkaTemplate,
            circuitBreaker,
            rateLimiter,
            executor,
            jobRepo
        );

        Map<String, Object> response = service.startPopulationCalculation(
            "tenant-1",
            "http://fhir.test",
            "user-1",
            null,
            null
        );
        String jobId = (String) response.get("jobId");
        Thread.sleep(5); // Wait for async execution

        PopulationCalculationService.BatchCalculationJob job = service.getJobStatus(jobId);
        assertThat(job).isNotNull();
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.FAILED);
        assertThat(job.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle invalid patient IDs from FHIR")
    void shouldHandleInvalidPatientIdsFromFhir() throws Exception {
        MeasureCalculationService measureCalculationService = mock(MeasureCalculationService.class);
        MeasureRegistry measureRegistry = mock(MeasureRegistry.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("measure-calc");
        RateLimiter rateLimiter = mock(RateLimiter.class);

        when(measureRegistry.getMeasureIds()).thenReturn(List.of("measure-1"));
        Map<String, Object> bundle = Map.of(
            "entry", List.of(Map.of("resource", Map.of("id", "not-a-uuid")))
        );
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(bundle);

        com.healthdata.quality.persistence.JobExecutionRepository jobRepo =
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(any())).thenReturn(java.util.Optional.empty());

        PopulationCalculationService service = new PopulationCalculationService(
            measureCalculationService,
            measureRegistry,
            restTemplate,
            kafkaTemplate,
            circuitBreaker,
            rateLimiter,
            Runnable::run,
            jobRepo
        );

        Map<String, Object> response = service.startPopulationCalculation(
            "tenant-1",
            "http://fhir.test",
            "user-1",
            null,
            null
        );
        String jobId = (String) response.get("jobId");
        Thread.sleep(2); // Wait for async execution

        PopulationCalculationService.BatchCalculationJob job = service.getJobStatus(jobId);
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.FAILED);
        assertThat(job.getErrors()).anyMatch(error -> error.contains("Failed to fetch patients"));
    }

    @Test
    @DisplayName("Should complete when no patients are returned")
    void shouldCompleteWhenNoPatientsReturned() throws Exception {
        MeasureCalculationService measureCalculationService = mock(MeasureCalculationService.class);
        MeasureRegistry measureRegistry = mock(MeasureRegistry.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("measure-calc");
        RateLimiter rateLimiter = mock(RateLimiter.class);

        when(measureRegistry.getMeasureIds()).thenReturn(List.of("measure-1"));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());

        com.healthdata.quality.persistence.JobExecutionRepository jobRepo =
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(any())).thenReturn(java.util.Optional.empty());

        PopulationCalculationService service = new PopulationCalculationService(
            measureCalculationService,
            measureRegistry,
            restTemplate,
            kafkaTemplate,
            circuitBreaker,
            rateLimiter,
            Runnable::run,
            jobRepo
        );

        Map<String, Object> response = service.startPopulationCalculation(
            "tenant-1",
            "http://fhir.test",
            "user-1",
            null,
            null
        );
        String jobId = (String) response.get("jobId");
        Thread.sleep(2); // Wait for async execution

        PopulationCalculationService.BatchCalculationJob job = service.getJobStatus(jobId);
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.COMPLETED);
        assertThat(job.getTotalPatients()).isZero();
        assertThat(job.getCompletedCalculations()).isZero();
    }

    @Test
    @DisplayName("Should record failed calculations and continue")
    void shouldRecordFailedCalculationsAndContinue() throws Exception {
        MeasureCalculationService measureCalculationService = mock(MeasureCalculationService.class);
        MeasureRegistry measureRegistry = mock(MeasureRegistry.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("measure-calc");
        RateLimiter rateLimiter = mock(RateLimiter.class);
        Executor executor = Runnable::run;

        when(rateLimiter.acquirePermission()).thenReturn(true);
        when(measureRegistry.getMeasureIds()).thenReturn(List.of("measure-1"));

        UUID patientId1 = UUID.randomUUID();
        UUID patientId2 = UUID.randomUUID();
        Map<String, Object> bundle = Map.of(
            "entry", List.of(
                Map.of("resource", Map.of("id", patientId1.toString())),
                Map.of("resource", Map.of("id", patientId2.toString()))
            )
        );
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(bundle);
        // First patient fails, second succeeds
        org.mockito.Mockito.doAnswer(invocation -> {
            UUID patient = invocation.getArgument(1);
            if (patient.equals(patientId1)) {
                throw new RuntimeException("calc failed");
            }
            return null; // void method
        }).when(measureCalculationService).calculateMeasure(eq("tenant-1"), any(UUID.class), anyString(), anyString());

        com.healthdata.quality.persistence.JobExecutionRepository jobRepo =
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class);
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepo.findById(any())).thenReturn(java.util.Optional.empty());

        PopulationCalculationService service = new PopulationCalculationService(
            measureCalculationService,
            measureRegistry,
            restTemplate,
            kafkaTemplate,
            circuitBreaker,
            rateLimiter,
            executor,
            jobRepo
        );

        Map<String, Object> response = service.startPopulationCalculation(
            "tenant-1",
            "http://fhir.test",
            "user-1",
            null,
            null
        );
        String jobId = (String) response.get("jobId");
        Thread.sleep(5); // Wait for async execution (increased for 2 patients)

        PopulationCalculationService.BatchCalculationJob job = service.getJobStatus(jobId);
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.COMPLETED);
        assertThat(job.getTotalPatients()).isEqualTo(2);
        assertThat(job.getCompletedCalculations()).isEqualTo(2);
        assertThat(job.getSuccessfulCalculations()).isEqualTo(1);
        assertThat(job.getFailedCalculations()).isEqualTo(1);
        assertThat(job.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should cancel a running job")
    void shouldCancelRunningJob() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        PopulationCalculationService service = new PopulationCalculationService(
            mock(MeasureCalculationService.class),
            mock(MeasureRegistry.class),
            mock(RestTemplate.class),
            kafkaTemplate,
            CircuitBreaker.ofDefaults("measure-calc"),
            mock(RateLimiter.class),
            Runnable::run,
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user-1");
        job.updateStatus(PopulationCalculationService.JobStatus.CALCULATING);

        java.lang.reflect.Field field = PopulationCalculationService.class.getDeclaredField("activeJobs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, PopulationCalculationService.BatchCalculationJob> activeJobs =
            (Map<String, PopulationCalculationService.BatchCalculationJob>) field.get(service);
        activeJobs.put("job-1", job);

        boolean cancelled = service.cancelJob("job-1");

        assertThat(cancelled).isTrue();
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should return active jobs filtered by tenant")
    void shouldReturnActiveJobsFilteredByTenant() throws Exception {
        PopulationCalculationService service = new PopulationCalculationService(
            mock(MeasureCalculationService.class),
            mock(MeasureRegistry.class),
            mock(RestTemplate.class),
            mock(KafkaTemplate.class),
            CircuitBreaker.ofDefaults("measure-calc"),
            mock(RateLimiter.class),
            Runnable::run,
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class)
        );

        PopulationCalculationService.BatchCalculationJob job1 =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        PopulationCalculationService.BatchCalculationJob job2 =
            new PopulationCalculationService.BatchCalculationJob("job-2", "tenant-2", "user");

        java.lang.reflect.Field field = PopulationCalculationService.class.getDeclaredField("activeJobs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, PopulationCalculationService.BatchCalculationJob> activeJobs =
            (Map<String, PopulationCalculationService.BatchCalculationJob>) field.get(service);
        activeJobs.put("job-1", job1);
        activeJobs.put("job-2", job2);

        List<PopulationCalculationService.BatchCalculationJob> result =
            service.getActiveJobs("tenant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("Should not cancel job when status not calculating")
    void shouldNotCancelWhenStatusNotCalculating() throws Exception {
        PopulationCalculationService service = new PopulationCalculationService(
            mock(MeasureCalculationService.class),
            mock(MeasureRegistry.class),
            mock(RestTemplate.class),
            mock(KafkaTemplate.class),
            CircuitBreaker.ofDefaults("measure-calc"),
            mock(RateLimiter.class),
            Runnable::run,
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        job.updateStatus(PopulationCalculationService.JobStatus.COMPLETED);

        java.lang.reflect.Field field = PopulationCalculationService.class.getDeclaredField("activeJobs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, PopulationCalculationService.BatchCalculationJob> activeJobs =
            (Map<String, PopulationCalculationService.BatchCalculationJob>) field.get(service);
        activeJobs.put("job-1", job);

        assertThat(service.cancelJob("job-1")).isFalse();
        assertThat(service.cancelJob("missing")).isFalse();
    }

    @Test
    @DisplayName("Should cancel job even if progress publish fails")
    void shouldCancelJobEvenIfPublishFails() throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        org.mockito.Mockito.doThrow(new RuntimeException("kafka down"))
            .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        PopulationCalculationService service = new PopulationCalculationService(
            mock(MeasureCalculationService.class),
            mock(MeasureRegistry.class),
            mock(RestTemplate.class),
            kafkaTemplate,
            CircuitBreaker.ofDefaults("measure-calc"),
            mock(RateLimiter.class),
            Runnable::run,
            mock(com.healthdata.quality.persistence.JobExecutionRepository.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user-1");
        job.updateStatus(PopulationCalculationService.JobStatus.CALCULATING);
        job.setTotalCalculations(100);
        job.setCompletedCalculations(10);

        java.lang.reflect.Field field = PopulationCalculationService.class.getDeclaredField("activeJobs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, PopulationCalculationService.BatchCalculationJob> activeJobs =
            (Map<String, PopulationCalculationService.BatchCalculationJob>) field.get(service);
        activeJobs.put("job-1", job);

        assertThat(service.cancelJob("job-1")).isTrue();
        assertThat(job.getStatus()).isEqualTo(PopulationCalculationService.JobStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should calculate progress and enforce error cap")
    void shouldCalculateProgressAndEnforceErrorCap() {
        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-3", "tenant-1", "user");

        job.setTotalCalculations(0);
        assertThat(job.getProgressPercent()).isEqualTo(0);

        job.setTotalCalculations(200);
        job.setCompletedCalculations(50);
        assertThat(job.getProgressPercent()).isEqualTo(25);

        for (int i = 0; i < 150; i++) {
            job.addError("error-" + i);
        }
        assertThat(job.getErrors()).hasSize(100);

        Duration duration = job.getDuration();
        assertThat(duration.toMillis()).isGreaterThanOrEqualTo(0);
    }
}
