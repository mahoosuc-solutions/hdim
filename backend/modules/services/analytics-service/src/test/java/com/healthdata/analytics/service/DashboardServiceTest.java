package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.dto.WidgetDto;
import com.healthdata.analytics.persistence.DashboardEntity;
import com.healthdata.analytics.persistence.DashboardWidgetEntity;
import com.healthdata.analytics.repository.DashboardRepository;
import com.healthdata.analytics.repository.DashboardWidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardService.
 * Tests dashboard and widget CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard Service Tests")
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DashboardWidgetRepository widgetRepository;

    private DashboardService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID DASHBOARD_ID = UUID.randomUUID();
    private static final UUID WIDGET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DashboardService(dashboardRepository, widgetRepository);
    }

    @Nested
    @DisplayName("Get Dashboards Tests")
    class GetDashboardsTests {

        @Test
        @DisplayName("Should return all dashboards for tenant")
        void shouldReturnAllDashboards() {
            // Given
            List<DashboardEntity> entities = List.of(
                    createDashboard("Dashboard 1", false),
                    createDashboard("Dashboard 2", true)
            );
            when(dashboardRepository.findByTenantId(TENANT_ID)).thenReturn(entities);

            // When
            List<DashboardDto> result = service.getDashboards(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Dashboard 1");
            assertThat(result.get(1).getName()).isEqualTo("Dashboard 2");
        }

        @Test
        @DisplayName("Should return empty list when no dashboards exist")
        void shouldReturnEmptyList() {
            // Given
            when(dashboardRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            // When
            List<DashboardDto> result = service.getDashboards(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated dashboards")
        void shouldReturnPaginatedDashboards() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<DashboardEntity> entities = List.of(createDashboard("Dashboard 1", false));
            Page<DashboardEntity> page = new PageImpl<>(entities, pageable, 1);
            when(dashboardRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<DashboardDto> result = service.getDashboardsPaginated(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Single Dashboard Tests")
    class GetSingleDashboardTests {

        @Test
        @DisplayName("Should return dashboard by ID")
        void shouldReturnDashboardById() {
            // Given
            DashboardEntity entity = createDashboard("My Dashboard", true);
            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));
            when(widgetRepository.findByDashboardIdAndTenantIdOrderByPositionYAscPositionXAsc(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(List.of());

            // When
            Optional<DashboardDto> result = service.getDashboard(DASHBOARD_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("My Dashboard");
            assertThat(result.get().getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("Should return empty when dashboard not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<DashboardDto> result = service.getDashboard(DASHBOARD_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should include widgets when fetching dashboard")
        void shouldIncludeWidgets() {
            // Given
            DashboardEntity dashboard = createDashboard("Dashboard with Widgets", false);
            List<DashboardWidgetEntity> widgets = List.of(
                    createWidget("Chart 1", "CHART"),
                    createWidget("KPI 1", "KPI")
            );

            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.of(dashboard));
            when(widgetRepository.findByDashboardIdAndTenantIdOrderByPositionYAscPositionXAsc(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(widgets);

            // When
            Optional<DashboardDto> result = service.getDashboard(DASHBOARD_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWidgets()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Accessible Dashboards Tests")
    class GetAccessibleDashboardsTests {

        @Test
        @DisplayName("Should return dashboards accessible to user")
        void shouldReturnAccessibleDashboards() {
            // Given
            List<DashboardEntity> entities = List.of(
                    createDashboard("My Dashboard", false),
                    createDashboard("Shared Dashboard", true)
            );
            when(dashboardRepository.findAccessibleDashboards(TENANT_ID, USER_ID)).thenReturn(entities);

            // When
            List<DashboardDto> result = service.getAccessibleDashboards(TENANT_ID, USER_ID);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Create Dashboard Tests")
    class CreateDashboardTests {

        @Test
        @DisplayName("Should create dashboard successfully")
        void shouldCreateDashboard() {
            // Given
            DashboardDto dto = DashboardDto.builder()
                    .name("New Dashboard")
                    .description("Description")
                    .isDefault(false)
                    .isShared(true)
                    .build();

            DashboardEntity savedEntity = createDashboard("New Dashboard", false);
            when(dashboardRepository.save(any(DashboardEntity.class))).thenReturn(savedEntity);

            // When
            DashboardDto result = service.createDashboard(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getName()).isEqualTo("New Dashboard");
            verify(dashboardRepository).save(any(DashboardEntity.class));
        }

        @Test
        @DisplayName("Should clear existing default when creating new default dashboard")
        void shouldClearExistingDefault() {
            // Given
            DashboardDto dto = DashboardDto.builder()
                    .name("New Default")
                    .isDefault(true)
                    .build();

            DashboardEntity existingDefault = createDashboard("Old Default", true);
            when(dashboardRepository.findByTenantIdAndIsDefaultTrue(TENANT_ID))
                    .thenReturn(Optional.of(existingDefault));
            when(dashboardRepository.save(any(DashboardEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDashboard(dto, TENANT_ID, USER_ID);

            // Then
            verify(dashboardRepository, times(2)).save(any(DashboardEntity.class));
        }

        @Test
        @DisplayName("Should set createdBy to current user")
        void shouldSetCreatedBy() {
            // Given
            DashboardDto dto = DashboardDto.builder()
                    .name("User Dashboard")
                    .build();

            ArgumentCaptor<DashboardEntity> captor = ArgumentCaptor.forClass(DashboardEntity.class);
            when(dashboardRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createDashboard(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Update Dashboard Tests")
    class UpdateDashboardTests {

        @Test
        @DisplayName("Should update dashboard successfully")
        void shouldUpdateDashboard() {
            // Given
            DashboardEntity existing = createDashboard("Old Name", false);
            DashboardDto dto = DashboardDto.builder()
                    .name("Updated Name")
                    .description("New Description")
                    .isDefault(false)
                    .isShared(true)
                    .build();

            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(dashboardRepository.save(any(DashboardEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<DashboardDto> result = service.updateDashboard(DASHBOARD_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should return empty when dashboard not found")
        void shouldReturnEmptyWhenUpdatingNonExistent() {
            // Given
            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<DashboardDto> result = service.updateDashboard(DASHBOARD_ID, new DashboardDto(), TENANT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(dashboardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should clear existing default when making dashboard default")
        void shouldClearDefaultWhenMakingDefault() {
            // Given
            DashboardEntity existing = createDashboard("Non-default", false);
            DashboardEntity existingDefault = createDashboard("Existing Default", true);
            DashboardDto dto = DashboardDto.builder()
                    .name("New Default")
                    .isDefault(true)
                    .build();

            when(dashboardRepository.findByIdAndTenantId(DASHBOARD_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(dashboardRepository.findByTenantIdAndIsDefaultTrue(TENANT_ID))
                    .thenReturn(Optional.of(existingDefault));
            when(dashboardRepository.save(any(DashboardEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.updateDashboard(DASHBOARD_ID, dto, TENANT_ID);

            // Then
            verify(dashboardRepository, times(2)).save(any(DashboardEntity.class));
        }
    }

    @Nested
    @DisplayName("Delete Dashboard Tests")
    class DeleteDashboardTests {

        @Test
        @DisplayName("Should delete dashboard successfully")
        void shouldDeleteDashboard() {
            // Given
            when(dashboardRepository.existsByIdAndTenantId(DASHBOARD_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteDashboard(DASHBOARD_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(widgetRepository).deleteByDashboardIdAndTenantId(DASHBOARD_ID, TENANT_ID);
            verify(dashboardRepository).deleteById(DASHBOARD_ID);
        }

        @Test
        @DisplayName("Should return false when dashboard not found")
        void shouldReturnFalseWhenNotFound() {
            // Given
            when(dashboardRepository.existsByIdAndTenantId(DASHBOARD_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteDashboard(DASHBOARD_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(dashboardRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Widget Management Tests")
    class WidgetManagementTests {

        @Test
        @DisplayName("Should add widget to dashboard")
        void shouldAddWidget() {
            // Given
            WidgetDto dto = WidgetDto.builder()
                    .dashboardId(DASHBOARD_ID)
                    .widgetType("CHART")
                    .title("Sales Chart")
                    .positionX(0)
                    .positionY(0)
                    .width(4)
                    .height(3)
                    .build();

            DashboardWidgetEntity savedEntity = createWidget("Sales Chart", "CHART");
            when(widgetRepository.save(any(DashboardWidgetEntity.class))).thenReturn(savedEntity);

            // When
            WidgetDto result = service.addWidget(dto, TENANT_ID);

            // Then
            assertThat(result.getTitle()).isEqualTo("Sales Chart");
            assertThat(result.getWidgetType()).isEqualTo("CHART");
            verify(widgetRepository).save(any(DashboardWidgetEntity.class));
        }

        @Test
        @DisplayName("Should update widget successfully")
        void shouldUpdateWidget() {
            // Given
            DashboardWidgetEntity existing = createWidget("Old Title", "KPI");
            WidgetDto dto = WidgetDto.builder()
                    .widgetType("CHART")
                    .title("New Title")
                    .positionX(1)
                    .positionY(2)
                    .width(6)
                    .height(4)
                    .build();

            when(widgetRepository.findByIdAndTenantId(WIDGET_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(widgetRepository.save(any(DashboardWidgetEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<WidgetDto> result = service.updateWidget(WIDGET_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("New Title");
            assertThat(result.get().getWidgetType()).isEqualTo("CHART");
        }

        @Test
        @DisplayName("Should return empty when updating non-existent widget")
        void shouldReturnEmptyWhenWidgetNotFound() {
            // Given
            when(widgetRepository.findByIdAndTenantId(WIDGET_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<WidgetDto> result = service.updateWidget(WIDGET_ID, new WidgetDto(), TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should delete widget successfully")
        void shouldDeleteWidget() {
            // Given
            when(widgetRepository.existsByIdAndTenantId(WIDGET_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteWidget(WIDGET_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(widgetRepository).deleteById(WIDGET_ID);
        }

        @Test
        @DisplayName("Should return false when deleting non-existent widget")
        void shouldReturnFalseWhenWidgetNotFound() {
            // Given
            when(widgetRepository.existsByIdAndTenantId(WIDGET_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteWidget(WIDGET_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(widgetRepository, never()).deleteById(any());
        }
    }

    // ==================== Helper Methods ====================

    private DashboardEntity createDashboard(String name, boolean isDefault) {
        return DashboardEntity.builder()
                .id(DASHBOARD_ID)
                .tenantId(TENANT_ID)
                .name(name)
                .description("Test dashboard")
                .isDefault(isDefault)
                .isShared(false)
                .createdBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private DashboardWidgetEntity createWidget(String title, String type) {
        return DashboardWidgetEntity.builder()
                .id(WIDGET_ID)
                .dashboardId(DASHBOARD_ID)
                .tenantId(TENANT_ID)
                .title(title)
                .widgetType(type)
                .positionX(0)
                .positionY(0)
                .width(4)
                .height(3)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
