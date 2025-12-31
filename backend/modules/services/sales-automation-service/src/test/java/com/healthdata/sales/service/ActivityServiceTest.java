package com.healthdata.sales.service;

import com.healthdata.sales.dto.ActivityDTO;
import com.healthdata.sales.entity.Activity;
import com.healthdata.sales.entity.ActivityType;
import com.healthdata.sales.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityService activityService;

    private UUID tenantId;
    private UUID activityId;
    private UUID leadId;
    private UUID contactId;
    private UUID accountId;
    private UUID opportunityId;
    private UUID userId;
    private Activity testActivity;
    private ActivityDTO testActivityDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        opportunityId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testActivity = createTestActivity();
        testActivityDTO = createTestActivityDTO();
    }

    private Activity createTestActivity() {
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setTenantId(tenantId);
        activity.setLeadId(leadId);
        activity.setContactId(contactId);
        activity.setAccountId(accountId);
        activity.setOpportunityId(opportunityId);
        activity.setActivityType(ActivityType.CALL);
        activity.setSubject("Discovery call with CMO");
        activity.setDescription("Initial discovery call to understand quality measurement needs");
        activity.setScheduledAt(LocalDateTime.now().plusDays(1));
        activity.setDurationMinutes(30);
        activity.setCompleted(false);
        activity.setAssignedToUserId(userId);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        return activity;
    }

    private ActivityDTO createTestActivityDTO() {
        return ActivityDTO.builder()
            .id(activityId)
            .tenantId(tenantId)
            .leadId(leadId)
            .contactId(contactId)
            .accountId(accountId)
            .opportunityId(opportunityId)
            .activityType(ActivityType.CALL)
            .subject("Discovery call with CMO")
            .description("Initial discovery call to understand quality measurement needs")
            .scheduledAt(LocalDateTime.now().plusDays(1))
            .durationMinutes(30)
            .isCompleted(false)
            .assignedToUserId(userId)
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return activity when found")
        void shouldReturnActivityWhenFound() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.of(testActivity));

            ActivityDTO result = activityService.findById(tenantId, activityId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(activityId);
            assertThat(result.getSubject()).isEqualTo("Discovery call with CMO");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.CALL);
            verify(activityRepository).findByIdAndTenantId(activityId, tenantId);
        }

        @Test
        @DisplayName("should throw RuntimeException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.findById(tenantId, activityId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated activities")
        void shouldReturnPaginatedActivities() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantId(tenantId, pageable)).thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new activity")
        void shouldCreateAndReturnNewActivity() {
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            ActivityDTO result = activityService.create(tenantId, testActivityDTO);

            assertThat(result).isNotNull();
            assertThat(result.getSubject()).isEqualTo("Discovery call with CMO");
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            verify(activityRepository).save(any(Activity.class));
        }

        @Test
        @DisplayName("should set completed to false by default")
        void shouldSetCompletedToFalseByDefault() {
            ActivityDTO dtoWithNullCompleted = ActivityDTO.builder()
                .subject("New Activity")
                .activityType(ActivityType.TASK)
                .build();

            Activity savedActivity = new Activity();
            savedActivity.setId(UUID.randomUUID());
            savedActivity.setTenantId(tenantId);
            savedActivity.setSubject("New Activity");
            savedActivity.setCompleted(false);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.create(tenantId, dtoWithNullCompleted);

            assertThat(result).isNotNull();
            assertThat(result.getIsCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing activity")
        void shouldUpdateExistingActivity() {
            ActivityDTO updateDTO = ActivityDTO.builder()
                .subject("Updated Subject")
                .description("Updated description")
                .build();

            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.of(testActivity));
            when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

            ActivityDTO result = activityService.update(tenantId, activityId, updateDTO);

            assertThat(result).isNotNull();
            verify(activityRepository).save(testActivity);
        }

        @Test
        @DisplayName("should throw RuntimeException when updating non-existent activity")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.update(tenantId, activityId, testActivityDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing activity")
        void shouldDeleteExistingActivity() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.of(testActivity));

            activityService.delete(tenantId, activityId);

            verify(activityRepository).delete(testActivity);
        }

        @Test
        @DisplayName("should throw RuntimeException when deleting non-existent activity")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.delete(tenantId, activityId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
        }
    }

    @Nested
    @DisplayName("markComplete")
    class MarkComplete {

        @Test
        @DisplayName("should mark activity as complete with outcome")
        void shouldMarkActivityAsComplete() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.of(testActivity));
            when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

            ActivityDTO result = activityService.markComplete(tenantId, activityId, "Successful call, scheduled demo");

            assertThat(result).isNotNull();
            verify(activityRepository).save(testActivity);
        }

        @Test
        @DisplayName("should throw RuntimeException when activity not found")
        void shouldThrowExceptionWhenActivityNotFound() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.markComplete(tenantId, activityId, "Outcome"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Activity not found");
        }
    }

    @Nested
    @DisplayName("findByLead")
    class FindByLead {

        @Test
        @DisplayName("should return activities for specific lead")
        void shouldReturnActivitiesForLead() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndLeadId(tenantId, leadId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByLead(tenantId, leadId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getLeadId()).isEqualTo(leadId);
        }
    }

    @Nested
    @DisplayName("findByContact")
    class FindByContact {

        @Test
        @DisplayName("should return activities for specific contact")
        void shouldReturnActivitiesForContact() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndContactId(tenantId, contactId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByContact(tenantId, contactId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContactId()).isEqualTo(contactId);
        }
    }

    @Nested
    @DisplayName("findByOpportunity")
    class FindByOpportunity {

        @Test
        @DisplayName("should return activities for specific opportunity")
        void shouldReturnActivitiesForOpportunity() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndOpportunityId(tenantId, opportunityId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByOpportunity(tenantId, opportunityId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOpportunityId()).isEqualTo(opportunityId);
        }
    }

    @Nested
    @DisplayName("findByAccount")
    class FindByAccount {

        @Test
        @DisplayName("should return activities for specific account")
        void shouldReturnActivitiesForAccount() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByAccount(tenantId, accountId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAccountId()).isEqualTo(accountId);
        }
    }

    @Nested
    @DisplayName("findByType")
    class FindByType {

        @Test
        @DisplayName("should return activities filtered by type")
        void shouldReturnActivitiesFilteredByType() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndActivityType(tenantId, ActivityType.CALL, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByType(tenantId, ActivityType.CALL, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getActivityType()).isEqualTo(ActivityType.CALL);
        }
    }

    @Nested
    @DisplayName("findByAssignedUser")
    class FindByAssignedUser {

        @Test
        @DisplayName("should return activities assigned to specific user")
        void shouldReturnActivitiesAssignedToUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findByTenantIdAndAssignedTo(tenantId, userId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findByAssignedUser(tenantId, userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAssignedToUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("findOverdueActivities")
    class FindOverdueActivities {

        @Test
        @DisplayName("should return overdue activities")
        void shouldReturnOverdueActivities() {
            Pageable pageable = PageRequest.of(0, 10);
            Activity overdueActivity = createTestActivity();
            overdueActivity.setScheduledAt(LocalDateTime.now().minusDays(1));
            overdueActivity.setCompleted(false);
            Page<Activity> activityPage = new PageImpl<>(List.of(overdueActivity), pageable, 1);

            when(activityRepository.findOverdueActivities(eq(tenantId), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findOverdueActivities(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findUpcomingActivities")
    class FindUpcomingActivities {

        @Test
        @DisplayName("should return upcoming activities within specified days")
        void shouldReturnUpcomingActivities() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findUpcomingActivities(eq(tenantId), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findUpcomingActivities(tenantId, 7, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findPendingActivitiesForUser")
    class FindPendingActivitiesForUser {

        @Test
        @DisplayName("should return pending activities for user")
        void shouldReturnPendingActivitiesForUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity), pageable, 1);

            when(activityRepository.findPendingActivitiesForUser(tenantId, userId, pageable))
                .thenReturn(activityPage);

            Page<ActivityDTO> result = activityService.findPendingActivitiesForUser(tenantId, userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getIsCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("countPendingActivities")
    class CountPendingActivities {

        @Test
        @DisplayName("should return count of pending activities")
        void shouldReturnCountOfPendingActivities() {
            when(activityRepository.countPendingActivities(tenantId)).thenReturn(5L);

            Long count = activityService.countPendingActivities(tenantId);

            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Activity Type Helpers")
    class ActivityTypeHelpers {

        @Test
        @DisplayName("logCall should create activity with CALL type")
        void logCallShouldCreateCallActivity() {
            ActivityDTO dto = ActivityDTO.builder()
                .subject("Follow-up call")
                .build();

            Activity savedActivity = createTestActivity();
            savedActivity.setActivityType(ActivityType.CALL);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.logCall(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getActivityType()).isEqualTo(ActivityType.CALL);
        }

        @Test
        @DisplayName("logEmail should create activity with EMAIL type")
        void logEmailShouldCreateEmailActivity() {
            ActivityDTO dto = ActivityDTO.builder()
                .subject("Introduction email")
                .build();

            Activity savedActivity = createTestActivity();
            savedActivity.setActivityType(ActivityType.EMAIL);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.logEmail(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getActivityType()).isEqualTo(ActivityType.EMAIL);
        }

        @Test
        @DisplayName("logMeeting should create activity with MEETING type")
        void logMeetingShouldCreateMeetingActivity() {
            ActivityDTO dto = ActivityDTO.builder()
                .subject("Quarterly review")
                .build();

            Activity savedActivity = createTestActivity();
            savedActivity.setActivityType(ActivityType.MEETING);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.logMeeting(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getActivityType()).isEqualTo(ActivityType.MEETING);
        }

        @Test
        @DisplayName("scheduleDemo should create activity with DEMO type")
        void scheduleDemoShouldCreateDemoActivity() {
            ActivityDTO dto = ActivityDTO.builder()
                .subject("Product demo")
                .build();

            Activity savedActivity = createTestActivity();
            savedActivity.setActivityType(ActivityType.DEMO);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.scheduleDemo(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getActivityType()).isEqualTo(ActivityType.DEMO);
        }

        @Test
        @DisplayName("createTask should create activity with TASK type")
        void createTaskShouldCreateTaskActivity() {
            ActivityDTO dto = ActivityDTO.builder()
                .subject("Send proposal")
                .build();

            Activity savedActivity = createTestActivity();
            savedActivity.setActivityType(ActivityType.TASK);

            when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

            ActivityDTO result = activityService.createTask(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getActivityType()).isEqualTo(ActivityType.TASK);
        }
    }

    @Nested
    @DisplayName("DTO Mapping")
    class DTOMapping {

        @Test
        @DisplayName("should correctly map all fields from entity to DTO")
        void shouldCorrectlyMapAllFieldsFromEntityToDTO() {
            when(activityRepository.findByIdAndTenantId(activityId, tenantId))
                .thenReturn(Optional.of(testActivity));

            ActivityDTO result = activityService.findById(tenantId, activityId);

            assertThat(result.getId()).isEqualTo(testActivity.getId());
            assertThat(result.getTenantId()).isEqualTo(testActivity.getTenantId());
            assertThat(result.getLeadId()).isEqualTo(testActivity.getLeadId());
            assertThat(result.getContactId()).isEqualTo(testActivity.getContactId());
            assertThat(result.getAccountId()).isEqualTo(testActivity.getAccountId());
            assertThat(result.getOpportunityId()).isEqualTo(testActivity.getOpportunityId());
            assertThat(result.getActivityType()).isEqualTo(testActivity.getActivityType());
            assertThat(result.getSubject()).isEqualTo(testActivity.getSubject());
            assertThat(result.getDescription()).isEqualTo(testActivity.getDescription());
            assertThat(result.getScheduledAt()).isEqualTo(testActivity.getScheduledAt());
            assertThat(result.getDurationMinutes()).isEqualTo(testActivity.getDurationMinutes());
            assertThat(result.getIsCompleted()).isEqualTo(testActivity.getCompleted());
            assertThat(result.getAssignedToUserId()).isEqualTo(testActivity.getAssignedToUserId());
        }
    }
}
