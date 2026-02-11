package com.healthdata.quality.persistence;

import com.healthdata.quality.config.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CareTeamMemberRepository
 */
@BaseIntegrationTest
class CareTeamMemberRepositoryTest {

    @Autowired
    private CareTeamMemberRepository repository;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final String USER_ID_1 = "user-1";
    private static final String USER_ID_2 = "user-2";
    private static final String USER_ID_3 = "user-3";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindCareTeamMember() {
        // Given
        CareTeamMemberEntity member = CareTeamMemberEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .userId(USER_ID_1)
            .role(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN)
            .isPrimary(true)
            .active(true)
            .startDate(LocalDateTime.now())
            .build();

        // When
        CareTeamMemberEntity saved = repository.save(member);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID_1);
        assertThat(saved.getRole()).isEqualTo(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN);
    }

    @Test
    void shouldFindActiveByPatientIdAndTenantId() {
        // Given
        CareTeamMemberEntity activeMember = createMember(USER_ID_1, true, null);
        CareTeamMemberEntity inactiveMember = createMember(USER_ID_2, false, null);
        CareTeamMemberEntity expiredMember = createMember(USER_ID_3, true, LocalDateTime.now().minusDays(1));

        repository.saveAll(List.of(activeMember, inactiveMember, expiredMember));

        // When
        List<CareTeamMemberEntity> activeMembers = repository.findActiveByPatientIdAndTenantId(
            PATIENT_ID, TENANT_ID
        );

        // Then
        assertThat(activeMembers).hasSize(1);
        assertThat(activeMembers.get(0).getUserId()).isEqualTo(USER_ID_1);
    }

    @Test
    void shouldFindPrimaryByPatientIdAndTenantId() {
        // Given
        CareTeamMemberEntity primaryMember = createMember(USER_ID_1, true, null);
        primaryMember.setIsPrimary(true);
        primaryMember.setRole(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN);

        CareTeamMemberEntity secondaryMember = createMember(USER_ID_2, true, null);
        secondaryMember.setIsPrimary(false);
        secondaryMember.setRole(CareTeamMemberEntity.CareTeamRole.REGISTERED_NURSE);

        repository.saveAll(List.of(primaryMember, secondaryMember));

        // When
        Optional<CareTeamMemberEntity> primary = repository.findPrimaryByPatientIdAndTenantId(
            PATIENT_ID, TENANT_ID
        );

        // Then
        assertThat(primary).isPresent();
        assertThat(primary.get().getUserId()).isEqualTo(USER_ID_1);
        assertThat(primary.get().getRole()).isEqualTo(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN);
    }

    @Test
    void shouldFindByUserIdAndTenantId() {
        // Given
        CareTeamMemberEntity member1 = createMember(USER_ID_1, true, null);
        member1.setPatientId(UUID.fromString("88888888-8888-8888-8888-888888888888"));

        CareTeamMemberEntity member2 = createMember(USER_ID_1, true, null);
        member2.setPatientId(UUID.fromString("99999999-9999-9999-9999-999999999999"));

        CareTeamMemberEntity member3 = createMember(USER_ID_2, true, null);

        repository.saveAll(List.of(member1, member2, member3));

        // When
        List<CareTeamMemberEntity> members = repository.findByUserIdAndTenantId(USER_ID_1, TENANT_ID);

        // Then
        assertThat(members).hasSize(2);
        assertThat(members).extracting(CareTeamMemberEntity::getPatientId)
            .containsExactlyInAnyOrder(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                UUID.fromString("99999999-9999-9999-9999-999999999999")
            );
    }

    @Test
    void shouldFindActiveByRole() {
        // Given
        CareTeamMemberEntity physician = createMember(USER_ID_1, true, null);
        physician.setRole(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN);

        CareTeamMemberEntity nurse = createMember(USER_ID_2, true, null);
        nurse.setRole(CareTeamMemberEntity.CareTeamRole.REGISTERED_NURSE);

        repository.saveAll(List.of(physician, nurse));

        // When
        List<CareTeamMemberEntity> physicians = repository.findActiveByPatientIdAndTenantIdAndRole(
            PATIENT_ID, TENANT_ID, CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN
        );

        // Then
        assertThat(physicians).hasSize(1);
        assertThat(physicians.get(0).getUserId()).isEqualTo(USER_ID_1);
    }

    @Test
    void shouldCountActiveMembers() {
        // Given
        repository.saveAll(List.of(
            createMember(USER_ID_1, true, null),
            createMember(USER_ID_2, true, null),
            createMember(USER_ID_3, false, null) // inactive
        ));

        // When
        long count = repository.countActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCheckCurrentlyActive() {
        // Given
        CareTeamMemberEntity activeMember = createMember(USER_ID_1, true, null);
        CareTeamMemberEntity inactiveMember = createMember(USER_ID_2, false, null);
        CareTeamMemberEntity expiredMember = createMember(USER_ID_3, true, LocalDateTime.now().minusDays(1));

        // When/Then
        assertThat(activeMember.isCurrentlyActive()).isTrue();
        assertThat(inactiveMember.isCurrentlyActive()).isFalse();
        assertThat(expiredMember.isCurrentlyActive()).isFalse();
    }

    @Test
    void shouldHandleFutureMembership() {
        // Given
        CareTeamMemberEntity futureMember = createMember(USER_ID_1, true, null);
        futureMember.setStartDate(LocalDateTime.now().plusDays(7));

        // When
        boolean isActive = futureMember.isCurrentlyActive();

        // Then
        assertThat(isActive).isFalse();
    }

    private CareTeamMemberEntity createMember(String userId, boolean active, LocalDateTime endDate) {
        return CareTeamMemberEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .userId(userId)
            .role(CareTeamMemberEntity.CareTeamRole.REGISTERED_NURSE)
            .isPrimary(false)
            .active(active)
            .startDate(LocalDateTime.now())
            .endDate(endDate)
            .build();
    }
}
