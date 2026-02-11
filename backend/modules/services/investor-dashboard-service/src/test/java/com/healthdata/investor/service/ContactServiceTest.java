package com.healthdata.investor.service;

import com.healthdata.investor.dto.ContactDTO;
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
 * Unit tests for ContactService.
 * Tests CRUD operations and contact filtering for investor contacts.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Contact Service Tests")
@Tag("unit")
class ContactServiceTest {

    @Mock
    private InvestorContactRepository contactRepository;

    @Mock
    private OutreachActivityRepository activityRepository;

    @Captor
    private ArgumentCaptor<InvestorContact> contactCaptor;

    private ContactService contactService;

    private static final UUID TEST_CONTACT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepository, activityRepository);
    }

    private InvestorContact createTestContact(UUID id, String name, String tier) {
        return InvestorContact.builder()
                .id(id)
                .name(name)
                .title("Partner")
                .organization("Venture Capital Inc")
                .email("contact@vc.com")
                .phone("555-1234")
                .linkedInUrl("https://linkedin.com/in/johndoe")
                .linkedInProfileId("johndoe")
                .category("VC")
                .status("identified")
                .tier(tier)
                .investmentThesis("Healthcare tech")
                .notes("Met at conference")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Get All Contacts Tests")
    class GetAllContactsTests {

        @Test
        @DisplayName("Should return all contacts ordered by tier and name")
        void shouldReturnAllContactsOrdered() {
            // Given
            List<InvestorContact> contacts = List.of(
                    createTestContact(UUID.randomUUID(), "Alice Adams", "A"),
                    createTestContact(UUID.randomUUID(), "Bob Brown", "B")
            );
            when(contactRepository.findAllByOrderByTierAscNameAsc()).thenReturn(contacts);

            // When
            List<ContactDTO> result = contactService.getAllContacts();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Alice Adams");
            assertThat(result.get(1).getName()).isEqualTo("Bob Brown");
        }

        @Test
        @DisplayName("Should return empty list when no contacts exist")
        void shouldReturnEmptyListWhenNoContacts() {
            // Given
            when(contactRepository.findAllByOrderByTierAscNameAsc()).thenReturn(List.of());

            // When
            List<ContactDTO> result = contactService.getAllContacts();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Contact By ID Tests")
    class GetContactByIdTests {

        @Test
        @DisplayName("Should return contact with activities when found")
        void shouldReturnContactWithActivities() {
            // Given
            InvestorContact contact = createTestContact(TEST_CONTACT_ID, "John Investor", "A");
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(activityRepository.findByContactIdOrderByActivityDateDesc(TEST_CONTACT_ID)).thenReturn(List.of());
            when(activityRepository.countByContactId(TEST_CONTACT_ID)).thenReturn(0L);

            // When
            ContactDTO result = contactService.getContact(TEST_CONTACT_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TEST_CONTACT_ID);
            assertThat(result.getName()).isEqualTo("John Investor");
            assertThat(result.getRecentActivities()).isEmpty();
        }

        @Test
        @DisplayName("Should return contact with limited recent activities")
        void shouldReturnContactWithLimitedActivities() {
            // Given
            InvestorContact contact = createTestContact(TEST_CONTACT_ID, "John Investor", "A");
            List<OutreachActivity> activities = List.of(
                    createTestActivity(contact, "email"),
                    createTestActivity(contact, "linkedin_message"),
                    createTestActivity(contact, "phone_call"),
                    createTestActivity(contact, "email"),
                    createTestActivity(contact, "email"),
                    createTestActivity(contact, "email") // 6th should be excluded
            );

            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(activityRepository.findByContactIdOrderByActivityDateDesc(TEST_CONTACT_ID)).thenReturn(activities);
            when(activityRepository.countByContactId(TEST_CONTACT_ID)).thenReturn(6L);

            // When
            ContactDTO result = contactService.getContact(TEST_CONTACT_ID);

            // Then
            assertThat(result.getRecentActivities()).hasSize(5); // Limited to 5
            assertThat(result.getActivityCount()).isEqualTo(6); // Total count is 6
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void shouldThrowWhenContactNotFound() {
            // Given
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> contactService.getContact(TEST_CONTACT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Contact");
        }
    }

    private OutreachActivity createTestActivity(InvestorContact contact, String activityType) {
        return OutreachActivity.builder()
                .id(UUID.randomUUID())
                .contact(contact)
                .activityType(activityType)
                .status("completed")
                .subject("Test activity")
                .activityDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Filter Contacts Tests")
    class FilterContactsTests {

        @Test
        @DisplayName("Should filter contacts by category")
        void shouldFilterByCategory() {
            // Given
            List<InvestorContact> vcContacts = List.of(
                    createTestContact(UUID.randomUUID(), "VC Contact", "A")
            );
            when(contactRepository.findByCategoryOrderByNameAsc("VC")).thenReturn(vcContacts);

            // When
            List<ContactDTO> result = contactService.getContactsByCategory("VC");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("VC");
        }

        @Test
        @DisplayName("Should filter contacts by status")
        void shouldFilterByStatus() {
            // Given
            List<InvestorContact> engagedContacts = List.of(
                    createTestContact(UUID.randomUUID(), "Engaged Contact", "A")
            );
            engagedContacts.get(0).setStatus("engaged");
            when(contactRepository.findByStatusOrderByNameAsc("engaged")).thenReturn(engagedContacts);

            // When
            List<ContactDTO> result = contactService.getContactsByStatus("engaged");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("engaged");
        }

        @Test
        @DisplayName("Should filter contacts by tier")
        void shouldFilterByTier() {
            // Given
            List<InvestorContact> tierAContacts = List.of(
                    createTestContact(UUID.randomUUID(), "Tier A Contact", "A")
            );
            when(contactRepository.findByTierOrderByNameAsc("A")).thenReturn(tierAContacts);

            // When
            List<ContactDTO> result = contactService.getContactsByTier("A");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTier()).isEqualTo("A");
        }

        @Test
        @DisplayName("Should search contacts by query")
        void shouldSearchContacts() {
            // Given
            List<InvestorContact> matchingContacts = List.of(
                    createTestContact(UUID.randomUUID(), "Sequoia Partner", "A")
            );
            when(contactRepository.findByNameContainingIgnoreCaseOrOrganizationContainingIgnoreCase("sequoia", "sequoia"))
                    .thenReturn(matchingContacts);

            // When
            List<ContactDTO> result = contactService.searchContacts("sequoia");

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get contacts needing follow-up")
        void shouldGetContactsNeedingFollowUp() {
            // Given
            InvestorContact contactNeedingFollowUp = createTestContact(UUID.randomUUID(), "Follow Up Contact", "B");
            contactNeedingFollowUp.setNextFollowUp(Instant.now().minusSeconds(86400)); // 1 day ago
            when(contactRepository.findContactsNeedingFollowUp()).thenReturn(List.of(contactNeedingFollowUp));

            // When
            List<ContactDTO> result = contactService.getContactsNeedingFollowUp();

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Create Contact Tests")
    class CreateContactTests {

        @Test
        @DisplayName("Should create contact with default tier B")
        void shouldCreateContactWithDefaultTier() {
            // Given
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> {
                InvestorContact contact = inv.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return contact;
            });

            ContactDTO.CreateRequest request = ContactDTO.CreateRequest.builder()
                    .name("New Contact")
                    .email("new@investor.com")
                    .category("Angel")
                    .build();

            // When
            ContactDTO result = contactService.createContact(request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getTier()).isEqualTo("B");
            assertThat(contactCaptor.getValue().getStatus()).isEqualTo("identified");
        }

        @Test
        @DisplayName("Should extract LinkedIn profile ID from URL")
        void shouldExtractLinkedInProfileId() {
            // Given
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> {
                InvestorContact contact = inv.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return contact;
            });

            ContactDTO.CreateRequest request = ContactDTO.CreateRequest.builder()
                    .name("LinkedIn Contact")
                    .linkedInUrl("https://www.linkedin.com/in/john-doe-investor/")
                    .build();

            // When
            contactService.createContact(request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getLinkedInProfileId()).isEqualTo("john-doe-investor");
        }

        @Test
        @DisplayName("Should handle LinkedIn URL without trailing slash")
        void shouldHandleLinkedInUrlWithoutSlash() {
            // Given
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> {
                InvestorContact contact = inv.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return contact;
            });

            ContactDTO.CreateRequest request = ContactDTO.CreateRequest.builder()
                    .name("LinkedIn Contact")
                    .linkedInUrl("https://linkedin.com/in/jane-smith")
                    .build();

            // When
            contactService.createContact(request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getLinkedInProfileId()).isEqualTo("jane-smith");
        }

        @Test
        @DisplayName("Should handle invalid LinkedIn URL gracefully")
        void shouldHandleInvalidLinkedInUrl() {
            // Given
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> {
                InvestorContact contact = inv.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return contact;
            });

            ContactDTO.CreateRequest request = ContactDTO.CreateRequest.builder()
                    .name("Contact")
                    .linkedInUrl("https://twitter.com/someuser")
                    .build();

            // When
            contactService.createContact(request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getLinkedInProfileId()).isNull();
        }

        @Test
        @DisplayName("Should use provided tier when specified")
        void shouldUseProvidedTier() {
            // Given
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> {
                InvestorContact contact = inv.getArgument(0);
                contact.setId(TEST_CONTACT_ID);
                return contact;
            });

            ContactDTO.CreateRequest request = ContactDTO.CreateRequest.builder()
                    .name("Contact")
                    .tier("A")
                    .build();

            // When
            contactService.createContact(request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getTier()).isEqualTo("A");
        }
    }

    @Nested
    @DisplayName("Update Contact Tests")
    class UpdateContactTests {

        @Test
        @DisplayName("Should update contact fields selectively")
        void shouldUpdateFieldsSelectively() {
            // Given
            InvestorContact existingContact = createTestContact(TEST_CONTACT_ID, "Original Name", "B");
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(existingContact));
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> inv.getArgument(0));

            ContactDTO.UpdateRequest request = ContactDTO.UpdateRequest.builder()
                    .name("Updated Name")
                    .build();

            // When
            ContactDTO result = contactService.updateContact(TEST_CONTACT_ID, request);

            // Then
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getOrganization()).isEqualTo("Venture Capital Inc"); // Unchanged
        }

        @Test
        @DisplayName("Should update LinkedIn profile ID when URL changes")
        void shouldUpdateLinkedInProfileIdOnUrlChange() {
            // Given
            InvestorContact existingContact = createTestContact(TEST_CONTACT_ID, "Contact", "B");
            existingContact.setLinkedInProfileId("old-profile");
            existingContact.setLinkedInUrl("https://linkedin.com/in/old-profile");

            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(existingContact));
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> inv.getArgument(0));

            ContactDTO.UpdateRequest request = ContactDTO.UpdateRequest.builder()
                    .linkedInUrl("https://linkedin.com/in/new-profile")
                    .build();

            // When
            contactService.updateContact(TEST_CONTACT_ID, request);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getLinkedInProfileId()).isEqualTo("new-profile");
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void shouldThrowWhenContactNotFound() {
            // Given
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.empty());

            ContactDTO.UpdateRequest request = ContactDTO.UpdateRequest.builder()
                    .name("Updated")
                    .build();

            // When/Then
            assertThatThrownBy(() -> contactService.updateContact(TEST_CONTACT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Contact Tests")
    class DeleteContactTests {

        @Test
        @DisplayName("Should delete contact when exists")
        void shouldDeleteContactWhenExists() {
            // Given
            when(contactRepository.existsById(TEST_CONTACT_ID)).thenReturn(true);

            // When
            contactService.deleteContact(TEST_CONTACT_ID);

            // Then
            verify(contactRepository).deleteById(TEST_CONTACT_ID);
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void shouldThrowWhenContactNotFound() {
            // Given
            when(contactRepository.existsById(TEST_CONTACT_ID)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> contactService.deleteContact(TEST_CONTACT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Last Contacted Tests")
    class UpdateLastContactedTests {

        @Test
        @DisplayName("Should update last contacted timestamp")
        void shouldUpdateLastContacted() {
            // Given
            InvestorContact contact = createTestContact(TEST_CONTACT_ID, "Contact", "B");
            contact.setLastContacted(null);

            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(InvestorContact.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            contactService.updateLastContacted(TEST_CONTACT_ID);

            // Then
            verify(contactRepository).save(contactCaptor.capture());
            assertThat(contactCaptor.getValue().getLastContacted()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void shouldThrowWhenContactNotFound() {
            // Given
            when(contactRepository.findById(TEST_CONTACT_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> contactService.updateLastContacted(TEST_CONTACT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
