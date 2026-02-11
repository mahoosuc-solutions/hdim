package com.healthdata.investor.service;

import com.healthdata.investor.dto.ActivityDTO;
import com.healthdata.investor.entity.InvestorContact;
import com.healthdata.investor.entity.OutreachActivity;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorContactRepository;
import com.healthdata.investor.repository.OutreachActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActivityService.
 * Tests CRUD operations and activity filtering for outreach activities.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Activity Service Tests")
@Tag("unit")
class ActivityServiceTest {

    @Mock
    private OutreachActivityRepository activityRepository;

    @Mock
    private InvestorContactRepository contactRepository;

    @Mock
    private ContactService contactService;

    @Captor
    private ArgumentCaptor<OutreachActivity> activityCaptor;

    @Captor
    private ArgumentCaptor<InvestorContact> contactCaptor;

    private ActivityService activityService;

    private static final UUID TEST_ACTIVITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final UUID TEST_CONTACT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID TEST_CREATED_BY = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(activityRepository, contactRepository, contactService);
    }

    private InvestorContact createTestContact() {
        return InvestorContact.builder()
                .id(TEST_CONTACT_ID)
                .name("Test Contact")
                .email("contact@test.com")
                .status("contacted")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private OutreachActivity createTestActivity(String activityType, String status) {
        return OutreachActivity.builder()
                .id(TEST_ACTIVITY_ID)
                .contact(createTestContact())
                .activityType(activityType)
                .status(status)
                .subject("Test Activity")
                .content("Activity content")
                .activityDate(LocalDate.now())
                .createdBy(TEST_CREATED_BY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Get All Activities Tests")
    class GetAllActivitiesTests {

        @Test
        @DisplayName("Should return all activities ordered by date")
        void shouldReturnAllActivitiesOrdered() {
            // Given
            List<OutreachActivity> activities = List.of(
                    createTestActivity("email", "completed"),
                    createTestActivity("phone_call", "pending")
            );
            when(activityRepository.findAllByOrderByActivityDateDesc()).thenReturn(activities);

            // When
            List<ActivityDTO> result = activityService.getAllActivities();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no activities exist")
        void shouldReturnEmptyListWhenNoActivities() {
            // Given
            when(activityRepository.findAllByOrderByActivityDateDesc()).thenReturn(List.of());

            // When
            List<ActivityDTO> result = activityService.getAllActivities();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Activity By ID Tests")
    class GetActivityByIdTests {

        @Test
        @DisplayName("Should return activity when found")
        void shouldReturnActivityWhenFound() {
            // Given
            OutreachActivity activity = createTestActivity("email", "completed");
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(activity));

            // When
            ActivityDTO result = activityService.getActivity(TEST_ACTIVITY_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TEST_ACTIVITY_ID);
            assertThat(result.getActivityType()).isEqualTo("email");
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowWhenActivityNotFound() {
            // Given
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> activityService.getActivity(TEST_ACTIVITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Activity");
        }
    }

    @Nested
    @DisplayName("Filter Activities Tests")
    class FilterActivitiesTests {

        @Test
        @DisplayName("Should filter activities by contact")
        void shouldFilterByContact() {
            // Given
            List<OutreachActivity> activities = List.of(createTestActivity("email", "completed"));
            when(activityRepository.findByContactIdOrderByActivityDateDesc(TEST_CONTACT_ID))
                    .thenReturn(activities);

            // When
            List<ActivityDTO> result = activityService.getActivitiesByContact(TEST_CONTACT_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContactId()).isEqualTo(TEST_CONTACT_ID);
        }

        @Test
        @DisplayName("Should filter activities by type")
        void shouldFilterByType() {
            // Given
            List<OutreachActivity> emailActivities = List.of(createTestActivity("email", "completed"));
            when(activityRepository.findByActivityTypeOrderByActivityDateDesc("email"))
                    .thenReturn(emailActivities);

            // When
            List<ActivityDTO> result = activityService.getActivitiesByType("email");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActivityType()).isEqualTo("email");
        }

        @Test
        @DisplayName("Should filter activities by date range")
        void shouldFilterByDateRange() {
            // Given
            LocalDate start = LocalDate.now().minusDays(7);
            LocalDate end = LocalDate.now();
            List<OutreachActivity> activities = List.of(createTestActivity("email", "completed"));
            when(activityRepository.findByActivityDateBetweenOrderByActivityDateDesc(start, end))
                    .thenReturn(activities);

            // When
            List<ActivityDTO> result = activityService.getActivitiesByDateRange(start, end);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get LinkedIn activities")
        void shouldGetLinkedInActivities() {
            // Given
            List<OutreachActivity> linkedInActivities = List.of(
                    createTestActivity("linkedin_message", "completed"),
                    createTestActivity("linkedin_connection", "pending")
            );
            when(activityRepository.findLinkedInActivities()).thenReturn(linkedInActivities);

            // When
            List<ActivityDTO> result = activityService.getLinkedInActivities();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should get pending scheduled activities")
        void shouldGetPendingScheduledActivities() {
            // Given
            List<OutreachActivity> pendingActivities = List.of(createTestActivity("email", "pending"));
            when(activityRepository.findPendingScheduledActivities()).thenReturn(pendingActivities);

            // When
            List<ActivityDTO> result = activityService.getPendingScheduledActivities();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("pending");
        }
    }

    @Nested
    @DisplayName("Create Activity Tests")
    class CreateActivityTests {

        @Test
        @DisplayName("Should create activity and update contact's lastContacted")
        void shouldCreateActivityAndUpdateContact() {
            // Given
            InvestorContact contact = createTestContact();
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> {
                OutreachActivity activity = inv.getArgument(0);
                activity.setId(TEST_ACTIVITY_ID);
                return activity;
            });

            ActivityDTO.CreateRequest request = ActivityDTO.CreateRequest.builder()
                    .contactId(TEST_CONTACT_ID)
                    .activityType("email")
                    .subject("Initial outreach")
                    .content("Email content")
                    .activityDate(LocalDate.now())
                    .build();

            // When
            ActivityDTO result = activityService.createActivity(request, TEST_CREATED_BY);

            // Then
            verify(activityRepository).save(activityCaptor.capture());
            verify(contactService).updateLastContacted(TEST_CONTACT_ID);

            OutreachActivity savedActivity = activityCaptor.getValue();
            assertThat(savedActivity.getStatus()).isEqualTo("pending");
            assertThat(savedActivity.getCreatedBy()).isEqualTo(TEST_CREATED_BY);
            assertThat(result.getActivityType()).isEqualTo("email");
        }

        @Test
        @DisplayName("Should set LinkedIn connection status for LinkedIn activities")
        void shouldSetLinkedInStatusForLinkedInActivities() {
            // Given
            InvestorContact contact = createTestContact();
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> {
                OutreachActivity activity = inv.getArgument(0);
                activity.setId(TEST_ACTIVITY_ID);
                return activity;
            });

            ActivityDTO.CreateRequest request = ActivityDTO.CreateRequest.builder()
                    .contactId(TEST_CONTACT_ID)
                    .activityType("linkedin_connection")
                    .subject("Connection request")
                    .activityDate(LocalDate.now())
                    .build();

            // When
            activityService.createActivity(request, TEST_CREATED_BY);

            // Then
            verify(activityRepository).save(activityCaptor.capture());
            assertThat(activityCaptor.getValue().getLinkedInConnectionStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("Should not set LinkedIn status for non-LinkedIn activities")
        void shouldNotSetLinkedInStatusForNonLinkedInActivities() {
            // Given
            InvestorContact contact = createTestContact();
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> {
                OutreachActivity activity = inv.getArgument(0);
                activity.setId(TEST_ACTIVITY_ID);
                return activity;
            });

            ActivityDTO.CreateRequest request = ActivityDTO.CreateRequest.builder()
                    .contactId(TEST_CONTACT_ID)
                    .activityType("email")
                    .subject("Email")
                    .activityDate(LocalDate.now())
                    .build();

            // When
            activityService.createActivity(request, TEST_CREATED_BY);

            // Then
            verify(activityRepository).save(activityCaptor.capture());
            assertThat(activityCaptor.getValue().getLinkedInConnectionStatus()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void shouldThrowWhenContactNotFound() {
            // Given
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.empty());

            ActivityDTO.CreateRequest request = ActivityDTO.CreateRequest.builder()
                    .contactId(TEST_CONTACT_ID)
                    .activityType("email")
                    .subject("Email")
                    .activityDate(LocalDate.now())
                    .build();

            // When/Then
            assertThatThrownBy(() -> activityService.createActivity(request, TEST_CREATED_BY))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Contact");
        }
    }

    @Nested
    @DisplayName("Update Activity Tests")
    class UpdateActivityTests {

        @Test
        @DisplayName("Should update activity fields selectively")
        void shouldUpdateFieldsSelectively() {
            // Given
            OutreachActivity existingActivity = createTestActivity("email", "pending");
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(existingActivity));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> inv.getArgument(0));

            ActivityDTO.UpdateRequest request = ActivityDTO.UpdateRequest.builder()
                    .status("completed")
                    .build();

            // When
            ActivityDTO result = activityService.updateActivity(TEST_ACTIVITY_ID, request);

            // Then
            assertThat(result.getStatus()).isEqualTo("completed");
            assertThat(result.getSubject()).isEqualTo("Test Activity"); // Unchanged
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowWhenActivityNotFound() {
            // Given
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.empty());

            ActivityDTO.UpdateRequest request = ActivityDTO.UpdateRequest.builder()
                    .status("completed")
                    .build();

            // When/Then
            assertThatThrownBy(() -> activityService.updateActivity(TEST_ACTIVITY_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Mark As Responded Tests")
    class MarkAsRespondedTests {

        @Test
        @DisplayName("Should mark activity as responded with timestamp")
        void shouldMarkAsRespondedWithTimestamp() {
            // Given
            OutreachActivity activity = createTestActivity("email", "pending");
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ActivityDTO result = activityService.markAsResponded(TEST_ACTIVITY_ID, "Thanks for reaching out!");

            // Then
            verify(activityRepository).save(activityCaptor.capture());
            OutreachActivity savedActivity = activityCaptor.getValue();
            assertThat(savedActivity.getStatus()).isEqualTo("responded");
            assertThat(savedActivity.getResponseReceived()).isNotNull();
            assertThat(savedActivity.getResponseContent()).isEqualTo("Thanks for reaching out!");
        }

        @Test
        @DisplayName("Should set LinkedIn connection status to connected for LinkedIn activities")
        void shouldSetLinkedInConnectedStatus() {
            // Given - activityType starts with "linkedin_" so isLinkedInActivity() returns true
            OutreachActivity activity = createTestActivity("linkedin_connection", "pending");
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            activityService.markAsResponded(TEST_ACTIVITY_ID, "Connected!");

            // Then
            verify(activityRepository).save(activityCaptor.capture());
            assertThat(activityCaptor.getValue().getLinkedInConnectionStatus()).isEqualTo("connected");
        }

        @Test
        @DisplayName("Should update contact status from contacted to engaged")
        void shouldUpdateContactStatusToEngaged() {
            // Given
            InvestorContact contact = createTestContact();
            contact.setStatus("contacted");

            OutreachActivity activity = OutreachActivity.builder()
                    .id(TEST_ACTIVITY_ID)
                    .contact(contact)
                    .activityType("email")
                    .status("pending")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            activityService.markAsResponded(TEST_ACTIVITY_ID, "Response received");

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getStatus()).isEqualTo("engaged");
        }

        @Test
        @DisplayName("Should not update contact status if not currently contacted")
        void shouldNotUpdateContactStatusIfNotContacted() {
            // Given
            InvestorContact contact = createTestContact();
            contact.setStatus("identified"); // Not "contacted"

            OutreachActivity activity = OutreachActivity.builder()
                    .id(TEST_ACTIVITY_ID)
                    .contact(contact)
                    .activityType("email")
                    .status("pending")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(activityRepository.save(any(OutreachActivity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            activityService.markAsResponded(TEST_ACTIVITY_ID, "Response");

            // Then
            verify(contactRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowWhenActivityNotFound() {
            // Given
            when(activityRepository.findById(TEST_ACTIVITY_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> activityService.markAsResponded(TEST_ACTIVITY_ID, "Response"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Activity Tests")
    class DeleteActivityTests {

        @Test
        @DisplayName("Should delete activity when exists")
        void shouldDeleteActivityWhenExists() {
            // Given
            when(activityRepository.existsById(TEST_ACTIVITY_ID)).thenReturn(true);

            // When
            activityService.deleteActivity(TEST_ACTIVITY_ID);

            // Then
            verify(activityRepository).deleteById(TEST_ACTIVITY_ID);
        }

        @Test
        @DisplayName("Should throw exception when activity not found")
        void shouldThrowWhenActivityNotFound() {
            // Given
            when(activityRepository.existsById(TEST_ACTIVITY_ID)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> activityService.deleteActivity(TEST_ACTIVITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(activityRepository, never()).deleteById(any());
        }
    }
}
