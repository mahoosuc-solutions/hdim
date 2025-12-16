package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.ReportDto;
import com.healthdata.analytics.dto.ReportExecutionDto;
import com.healthdata.analytics.persistence.ReportEntity;
import com.healthdata.analytics.persistence.ReportExecutionEntity;
import com.healthdata.analytics.repository.ReportExecutionRepository;
import com.healthdata.analytics.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportExecutionRepository executionRepository;
    private final KpiService kpiService;

    @Transactional(readOnly = true)
    public List<ReportDto> getReports(String tenantId) {
        return reportRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> getReportsPaginated(String tenantId, Pageable pageable) {
        return reportRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ReportDto> getReport(UUID id, String tenantId) {
        return reportRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDtoWithLatestExecution);
    }

    @Transactional
    public ReportDto createReport(ReportDto dto, String tenantId, String userId) {
        ReportEntity entity = ReportEntity.builder()
                .tenantId(tenantId)
                .name(dto.getName())
                .description(dto.getDescription())
                .reportType(dto.getReportType())
                .parameters(dto.getParameters())
                .scheduleCron(dto.getScheduleCron())
                .scheduleEnabled(dto.getScheduleEnabled() != null ? dto.getScheduleEnabled() : false)
                .outputFormat(dto.getOutputFormat() != null ? dto.getOutputFormat() : "PDF")
                .recipients(dto.getRecipients())
                .createdBy(userId)
                .build();

        entity = reportRepository.save(entity);
        log.info("Created report {} for tenant {}", entity.getId(), tenantId);
        return toDto(entity);
    }

    @Transactional
    public Optional<ReportDto> updateReport(UUID id, ReportDto dto, String tenantId) {
        return reportRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setName(dto.getName());
                    entity.setDescription(dto.getDescription());
                    entity.setReportType(dto.getReportType());
                    entity.setParameters(dto.getParameters());
                    entity.setScheduleCron(dto.getScheduleCron());
                    entity.setScheduleEnabled(dto.getScheduleEnabled());
                    entity.setOutputFormat(dto.getOutputFormat());
                    entity.setRecipients(dto.getRecipients());

                    entity = reportRepository.save(entity);
                    log.info("Updated report {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public boolean deleteReport(UUID id, String tenantId) {
        if (reportRepository.existsByIdAndTenantId(id, tenantId)) {
            reportRepository.deleteById(id);
            log.info("Deleted report {}", id);
            return true;
        }
        return false;
    }

    @Transactional
    public ReportExecutionDto executeReport(UUID reportId, String tenantId, String userId, Map<String, Object> parameters) {
        Optional<ReportEntity> reportOpt = reportRepository.findByIdAndTenantId(reportId, tenantId);
        if (reportOpt.isEmpty()) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        ReportEntity report = reportOpt.get();
        Map<String, Object> mergedParams = new HashMap<>();
        if (report.getParameters() != null) {
            mergedParams.putAll(report.getParameters());
        }
        if (parameters != null) {
            mergedParams.putAll(parameters);
        }

        ReportExecutionEntity execution = ReportExecutionEntity.builder()
                .reportId(reportId)
                .tenantId(tenantId)
                .status("PENDING")
                .parameters(mergedParams)
                .triggeredBy(userId)
                .build();

        execution = executionRepository.save(execution);
        log.info("Created execution {} for report {}", execution.getId(), reportId);

        executeReportAsync(execution.getId(), report, tenantId);

        return toExecutionDto(execution);
    }

    @Async
    public CompletableFuture<Void> executeReportAsync(UUID executionId, ReportEntity report, String tenantId) {
        ReportExecutionEntity execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            execution.setStatus("RUNNING");
            execution.setStartedAt(LocalDateTime.now());
            executionRepository.save(execution);

            Map<String, Object> resultData = generateReportData(report, tenantId);

            execution.setStatus("COMPLETED");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setResultData(resultData);
            execution.setRowCount(calculateRowCount(resultData));
            executionRepository.save(execution);

            log.info("Report execution {} completed successfully", executionId);
        } catch (Exception e) {
            log.error("Report execution {} failed: {}", executionId, e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional(readOnly = true)
    public List<ReportExecutionDto> getExecutions(UUID reportId, String tenantId) {
        return executionRepository.findByReportIdAndTenantId(reportId, tenantId)
                .stream()
                .map(this::toExecutionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReportExecutionDto> getExecutionsPaginated(UUID reportId, String tenantId, Pageable pageable) {
        return executionRepository.findByReportIdAndTenantId(reportId, tenantId, pageable)
                .map(this::toExecutionDto);
    }

    @Transactional(readOnly = true)
    public Optional<ReportExecutionDto> getExecution(UUID executionId, String tenantId) {
        return executionRepository.findByIdAndTenantId(executionId, tenantId)
                .map(this::toExecutionDto);
    }

    private Map<String, Object> generateReportData(ReportEntity report, String tenantId) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportName", report.getName());
        data.put("reportType", report.getReportType());
        data.put("generatedAt", LocalDateTime.now());

        switch (report.getReportType()) {
            case "QUALITY":
                data.put("kpis", kpiService.getQualityKpis(tenantId));
                break;
            case "HCC":
                data.put("kpis", kpiService.getHccKpis(tenantId));
                break;
            case "CARE_GAP":
                data.put("kpis", kpiService.getCareGapKpis(tenantId));
                break;
            default:
                data.put("kpis", kpiService.getAllKpis(tenantId));
        }

        return data;
    }

    private Integer calculateRowCount(Map<String, Object> resultData) {
        if (resultData == null) return 0;
        Object kpis = resultData.get("kpis");
        if (kpis instanceof List) {
            return ((List<?>) kpis).size();
        }
        return 1;
    }

    private ReportDto toDto(ReportEntity entity) {
        return ReportDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .reportType(entity.getReportType())
                .parameters(entity.getParameters())
                .scheduleCron(entity.getScheduleCron())
                .scheduleEnabled(entity.getScheduleEnabled())
                .outputFormat(entity.getOutputFormat())
                .recipients(entity.getRecipients())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ReportDto toDtoWithLatestExecution(ReportEntity entity) {
        ReportDto dto = toDto(entity);
        executionRepository.findLatestSuccessfulExecution(entity.getId())
                .ifPresent(exec -> dto.setLatestExecution(toExecutionDto(exec)));
        return dto;
    }

    private ReportExecutionDto toExecutionDto(ReportExecutionEntity entity) {
        return ReportExecutionDto.builder()
                .id(entity.getId())
                .reportId(entity.getReportId())
                .status(entity.getStatus())
                .parameters(entity.getParameters())
                .resultData(entity.getResultData())
                .resultFilePath(entity.getResultFilePath())
                .resultFileSize(entity.getResultFileSize())
                .rowCount(entity.getRowCount())
                .triggeredBy(entity.getTriggeredBy())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
