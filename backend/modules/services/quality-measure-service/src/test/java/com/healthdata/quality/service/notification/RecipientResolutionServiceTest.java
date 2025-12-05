package com.healthdata.quality.service.notification;

import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for RecipientResolutionService
 */
@ExtendWith(MockitoExtension.class)
class RecipientResolutionServiceTest {

    @Mock
    private CareTeamMemberRepository careTeamRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private RecipientResolutionService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String USER_ID_1 = "user-1";
    private static final String USER_ID_2 = "user-2";

    @BeforeEach
    void setUp() {
        // Setup is handled by @InjectMocks
    }

    @Test
    void shouldResolveRecipientsForPatient() {
        // Given
        CareTeamMemberEntity member1 = createCareTeamMember(USER_ID_1, true);
        CareTeamMemberEntity member2 = createCareTeamMember(USER_ID_2, false);

        NotificationPreferenceEntity pref1 = createPreference(USER_ID_1, "user1@example.com", "+15555551111");
        NotificationPreferenceEntity pref2 = createPreference(USER_ID_2, "user2@example.com", "+15555552222");

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member1, member2));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref1, pref2));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then
        assertThat(recipients).hasSize(2);
        assertThat(recipients).extracting(NotificationRecipient::getUserId)
            .containsExactlyInAnyOrder(USER_ID_1, USER_ID_2);
    }

    @Test
    void shouldFilterByChannel() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);
        NotificationPreferenceEntity pref = createPreference(USER_ID_1, "user1@example.com", null);
        pref.setSmsEnabled(false);

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.SMS,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then - SMS disabled, should filter out
        assertThat(recipients).isEmpty();
    }

    @Test
    void shouldRespectQuietHours() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);
        NotificationPreferenceEntity pref = createPreference(USER_ID_1, "user1@example.com", "+15555551111");
        pref.setQuietHoursEnabled(true);
        pref.setQuietHoursStart(LocalTime.of(0, 0)); // Always in quiet hours for test
        pref.setQuietHoursEnd(LocalTime.of(23, 59));
        pref.setQuietHoursOverrideCritical(false);

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.MEDIUM
        );

        // Then - should be filtered by quiet hours
        assertThat(recipients).isEmpty();
    }

    @Test
    void shouldOverrideQuietHoursForCritical() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);
        NotificationPreferenceEntity pref = createPreference(USER_ID_1, "user1@example.com", "+15555551111");
        pref.setQuietHoursEnabled(true);
        pref.setQuietHoursStart(LocalTime.of(0, 0));
        pref.setQuietHoursEnd(LocalTime.of(23, 59));
        pref.setQuietHoursOverrideCritical(true); // Critical override enabled

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.CRITICAL
        );

        // Then - should override quiet hours for CRITICAL
        assertThat(recipients).hasSize(1);
    }

    @Test
    void shouldFilterBySeverityThreshold() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);
        NotificationPreferenceEntity pref = createPreference(USER_ID_1, "user1@example.com", "+15555551111");
        pref.setSeverityThreshold(NotificationEntity.NotificationSeverity.HIGH);

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref));

        // When - send MEDIUM severity (below HIGH threshold)
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.MEDIUM
        );

        // Then - should be filtered out
        assertThat(recipients).isEmpty();
    }

    @Test
    void shouldIncludeOnlyConsentedUsers() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);
        NotificationPreferenceEntity pref = createPreference(USER_ID_1, "user1@example.com", "+15555551111");
        pref.setConsentGiven(false); // No consent

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then - should be filtered out (no consent)
        assertThat(recipients).isEmpty();
    }

    @Test
    void shouldHandleNoCareTeamMembers() {
        // Given
        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of());

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then
        assertThat(recipients).isEmpty();
        verify(preferenceRepository, never()).findByUserIdsAndTenantId(any(), any());
    }

    @Test
    void shouldHandleNoPreferences() {
        // Given
        CareTeamMemberEntity member = createCareTeamMember(USER_ID_1, true);

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(member));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of()); // No preferences found

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then
        assertThat(recipients).isEmpty();
    }

    @Test
    void shouldPrioritizePrimaryCareProvider() {
        // Given
        CareTeamMemberEntity primaryMember = createCareTeamMember(USER_ID_1, true);
        CareTeamMemberEntity secondaryMember = createCareTeamMember(USER_ID_2, false);

        NotificationPreferenceEntity pref1 = createPreference(USER_ID_1, "primary@example.com", "+15555551111");
        NotificationPreferenceEntity pref2 = createPreference(USER_ID_2, "secondary@example.com", "+15555552222");

        when(careTeamRepository.findActiveByPatientIdAndTenantId(PATIENT_ID, TENANT_ID))
            .thenReturn(List.of(primaryMember, secondaryMember));
        when(preferenceRepository.findByUserIdsAndTenantId(any(), eq(TENANT_ID)))
            .thenReturn(List.of(pref1, pref2));

        // When
        List<NotificationRecipient> recipients = service.resolveRecipients(
            TENANT_ID, PATIENT_ID, NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationSeverity.HIGH
        );

        // Then
        assertThat(recipients).hasSize(2);
        NotificationRecipient firstRecipient = recipients.get(0);
        assertThat(firstRecipient.isPrimary()).isTrue();
        assertThat(firstRecipient.getUserId()).isEqualTo(USER_ID_1);
    }

    private CareTeamMemberEntity createCareTeamMember(String userId, boolean isPrimary) {
        return CareTeamMemberEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .userId(userId)
            .role(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN)
            .isPrimary(isPrimary)
            .active(true)
            .startDate(LocalDateTime.now())
            .build();
    }

    private NotificationPreferenceEntity createPreference(String userId, String email, String phone) {
        return NotificationPreferenceEntity.builder()
            .tenantId(TENANT_ID)
            .userId(userId)
            .emailEnabled(true)
            .smsEnabled(true)
            .emailAddress(email)
            .phoneNumber(phone)
            .severityThreshold(NotificationEntity.NotificationSeverity.MEDIUM)
            .consentGiven(true)
            .consentDate(LocalDateTime.now())
            .build();
    }
}
