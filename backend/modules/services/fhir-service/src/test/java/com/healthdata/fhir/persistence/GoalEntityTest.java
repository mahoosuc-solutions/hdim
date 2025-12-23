package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GoalEntity Tests")
class GoalEntityTest {

    @Test
    @DisplayName("Should evaluate active lifecycle statuses")
    void shouldEvaluateActiveLifecycleStatuses() {
        GoalEntity active = GoalEntity.builder()
                .id(UUID.randomUUID())
                .lifecycleStatus("active")
                .build();
        GoalEntity accepted = GoalEntity.builder()
                .id(UUID.randomUUID())
                .lifecycleStatus("accepted")
                .build();
        GoalEntity planned = GoalEntity.builder()
                .id(UUID.randomUUID())
                .lifecycleStatus("planned")
                .build();

        assertThat(active.isActive()).isTrue();
        assertThat(accepted.isActive()).isTrue();
        assertThat(planned.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should evaluate achieved status")
    void shouldEvaluateAchievedStatus() {
        GoalEntity achieved = GoalEntity.builder()
                .id(UUID.randomUUID())
                .achievementStatus("achieved")
                .build();
        GoalEntity inProgress = GoalEntity.builder()
                .id(UUID.randomUUID())
                .achievementStatus("in-progress")
                .build();

        assertThat(achieved.isAchieved()).isTrue();
        assertThat(inProgress.isAchieved()).isFalse();
    }

    @Test
    @DisplayName("Should evaluate overdue status")
    void shouldEvaluateOverdueStatus() {
        GoalEntity overdue = GoalEntity.builder()
                .id(UUID.randomUUID())
                .targetDate(LocalDate.now().minusDays(1))
                .achievementStatus("in-progress")
                .lifecycleStatus("active")
                .build();
        GoalEntity achieved = GoalEntity.builder()
                .id(UUID.randomUUID())
                .targetDate(LocalDate.now().minusDays(1))
                .achievementStatus("achieved")
                .lifecycleStatus("active")
                .build();
        GoalEntity completed = GoalEntity.builder()
                .id(UUID.randomUUID())
                .targetDate(LocalDate.now().minusDays(1))
                .achievementStatus("in-progress")
                .lifecycleStatus("completed")
                .build();
        GoalEntity future = GoalEntity.builder()
                .id(UUID.randomUUID())
                .targetDate(LocalDate.now().plusDays(1))
                .achievementStatus("in-progress")
                .lifecycleStatus("active")
                .build();
        GoalEntity noTarget = GoalEntity.builder()
                .id(UUID.randomUUID())
                .achievementStatus("in-progress")
                .lifecycleStatus("active")
                .build();

        assertThat(overdue.isOverdue()).isTrue();
        assertThat(achieved.isOverdue()).isFalse();
        assertThat(completed.isOverdue()).isFalse();
        assertThat(future.isOverdue()).isFalse();
        assertThat(noTarget.isOverdue()).isFalse();
    }

    @Test
    @DisplayName("Should initialize timestamps on create")
    void shouldInitializeTimestampsOnCreate() {
        GoalEntity entity = GoalEntity.builder()
                .id(UUID.randomUUID())
                .build();

        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update last modified timestamp on update")
    void shouldUpdateLastModifiedTimestampOnUpdate() {
        GoalEntity entity = GoalEntity.builder()
                .id(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(60))
                .lastModifiedAt(Instant.now().minusSeconds(60))
                .build();

        entity.onUpdate();

        assertThat(entity.getLastModifiedAt()).isAfter(entity.getCreatedAt());
    }
}
