package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.dto.WidgetDto;
import com.healthdata.analytics.persistence.DashboardEntity;
import com.healthdata.analytics.persistence.DashboardWidgetEntity;
import com.healthdata.analytics.repository.DashboardRepository;
import com.healthdata.analytics.repository.DashboardWidgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository widgetRepository;

    @Transactional(readOnly = true)
    public List<DashboardDto> getDashboards(String tenantId) {
        return dashboardRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DashboardDto> getDashboardsPaginated(String tenantId, Pageable pageable) {
        return dashboardRepository.findByTenantId(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<DashboardDto> getDashboard(UUID id, String tenantId) {
        return dashboardRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDtoWithWidgets);
    }

    @Transactional(readOnly = true)
    public List<DashboardDto> getAccessibleDashboards(String tenantId, String userId) {
        return dashboardRepository.findAccessibleDashboards(tenantId, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DashboardDto createDashboard(DashboardDto dto, String tenantId, String userId) {
        DashboardEntity entity = DashboardEntity.builder()
                .tenantId(tenantId)
                .name(dto.getName())
                .description(dto.getDescription())
                .layout(dto.getLayout())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .isShared(dto.getIsShared() != null ? dto.getIsShared() : false)
                .createdBy(userId)
                .build();

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultDashboard(tenantId);
        }

        entity = dashboardRepository.save(entity);
        log.info("Created dashboard {} for tenant {}", entity.getId(), tenantId);
        return toDto(entity);
    }

    @Transactional
    public Optional<DashboardDto> updateDashboard(UUID id, DashboardDto dto, String tenantId) {
        return dashboardRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setName(dto.getName());
                    entity.setDescription(dto.getDescription());
                    entity.setLayout(dto.getLayout());
                    entity.setIsShared(dto.getIsShared());

                    if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
                        clearDefaultDashboard(tenantId);
                        entity.setIsDefault(true);
                    }

                    entity = dashboardRepository.save(entity);
                    log.info("Updated dashboard {}", id);
                    return toDto(entity);
                });
    }

    @Transactional
    public boolean deleteDashboard(UUID id, String tenantId) {
        if (dashboardRepository.existsByIdAndTenantId(id, tenantId)) {
            widgetRepository.deleteByDashboardIdAndTenantId(id, tenantId);
            dashboardRepository.deleteById(id);
            log.info("Deleted dashboard {}", id);
            return true;
        }
        return false;
    }

    @Transactional
    public WidgetDto addWidget(WidgetDto dto, String tenantId) {
        DashboardWidgetEntity entity = DashboardWidgetEntity.builder()
                .dashboardId(dto.getDashboardId())
                .tenantId(tenantId)
                .widgetType(dto.getWidgetType())
                .title(dto.getTitle())
                .config(dto.getConfig())
                .dataSource(dto.getDataSource())
                .refreshIntervalSeconds(dto.getRefreshIntervalSeconds())
                .positionX(dto.getPositionX())
                .positionY(dto.getPositionY())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .build();

        entity = widgetRepository.save(entity);
        log.info("Added widget {} to dashboard {}", entity.getId(), dto.getDashboardId());
        return toWidgetDto(entity);
    }

    @Transactional
    public Optional<WidgetDto> updateWidget(UUID id, WidgetDto dto, String tenantId) {
        return widgetRepository.findByIdAndTenantId(id, tenantId)
                .map(entity -> {
                    entity.setWidgetType(dto.getWidgetType());
                    entity.setTitle(dto.getTitle());
                    entity.setConfig(dto.getConfig());
                    entity.setDataSource(dto.getDataSource());
                    entity.setRefreshIntervalSeconds(dto.getRefreshIntervalSeconds());
                    entity.setPositionX(dto.getPositionX());
                    entity.setPositionY(dto.getPositionY());
                    entity.setWidth(dto.getWidth());
                    entity.setHeight(dto.getHeight());

                    entity = widgetRepository.save(entity);
                    log.info("Updated widget {}", id);
                    return toWidgetDto(entity);
                });
    }

    @Transactional
    public boolean deleteWidget(UUID id, String tenantId) {
        if (widgetRepository.existsByIdAndTenantId(id, tenantId)) {
            widgetRepository.deleteById(id);
            log.info("Deleted widget {}", id);
            return true;
        }
        return false;
    }

    private void clearDefaultDashboard(String tenantId) {
        dashboardRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .ifPresent(defaultDashboard -> {
                    defaultDashboard.setIsDefault(false);
                    dashboardRepository.save(defaultDashboard);
                });
    }

    private DashboardDto toDto(DashboardEntity entity) {
        return DashboardDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .layout(entity.getLayout())
                .isDefault(entity.getIsDefault())
                .isShared(entity.getIsShared())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DashboardDto toDtoWithWidgets(DashboardEntity entity) {
        DashboardDto dto = toDto(entity);
        List<WidgetDto> widgets = widgetRepository
                .findByDashboardIdAndTenantIdOrderByPositionYAscPositionXAsc(entity.getId(), entity.getTenantId())
                .stream()
                .map(this::toWidgetDto)
                .collect(Collectors.toList());
        dto.setWidgets(widgets);
        return dto;
    }

    private WidgetDto toWidgetDto(DashboardWidgetEntity entity) {
        return WidgetDto.builder()
                .id(entity.getId())
                .dashboardId(entity.getDashboardId())
                .widgetType(entity.getWidgetType())
                .title(entity.getTitle())
                .config(entity.getConfig())
                .dataSource(entity.getDataSource())
                .refreshIntervalSeconds(entity.getRefreshIntervalSeconds())
                .positionX(entity.getPositionX())
                .positionY(entity.getPositionY())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
