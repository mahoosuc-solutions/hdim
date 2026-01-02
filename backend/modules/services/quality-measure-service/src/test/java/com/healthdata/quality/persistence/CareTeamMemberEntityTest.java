package com.healthdata.quality.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CareTeamMemberEntity Tests")
class CareTeamMemberEntityTest {

    @Test
    @DisplayName("Should populate timestamps on create")
    void shouldPopulateTimestampsOnCreate() {
        CareTeamMemberEntity entity = CareTeamMemberEntity.builder()
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .userId("user-1")
            .role(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN)
            .build();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getStartDate()).isNotNull();
    }

    @Test
    @DisplayName("Should populate updatedAt on update")
    void shouldPopulateUpdatedAtOnUpdate() {
        CareTeamMemberEntity entity = new CareTeamMemberEntity();

        entity.onUpdate();

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should report inactive when deactivated or outside date range")
    void shouldReportInactiveWhenNotActiveOrOutOfRange() {
        CareTeamMemberEntity inactive = CareTeamMemberEntity.builder()
            .active(false)
            .build();
        assertThat(inactive.isCurrentlyActive()).isFalse();

        CareTeamMemberEntity futureStart = CareTeamMemberEntity.builder()
            .active(true)
            .startDate(LocalDateTime.now().plusDays(1))
            .build();
        assertThat(futureStart.isCurrentlyActive()).isFalse();

        CareTeamMemberEntity ended = CareTeamMemberEntity.builder()
            .active(true)
            .startDate(LocalDateTime.now().minusDays(2))
            .endDate(LocalDateTime.now().minusDays(1))
            .build();
        assertThat(ended.isCurrentlyActive()).isFalse();
    }

    @Test
    @DisplayName("Should report active when within date range")
    void shouldReportActiveWhenWithinDateRange() {
        CareTeamMemberEntity entity = CareTeamMemberEntity.builder()
            .active(true)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(1))
            .build();

        assertThat(entity.isCurrentlyActive()).isTrue();
    }
}
