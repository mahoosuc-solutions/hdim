package com.healthdata.sales.service;

import com.healthdata.sales.dto.ContactDTO;
import com.healthdata.sales.entity.Contact;
import com.healthdata.sales.entity.ContactType;
import com.healthdata.sales.repository.ContactRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    private UUID tenantId;
    private UUID contactId;
    private UUID accountId;
    private Contact testContact;
    private ContactDTO testContactDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testContact = createTestContact();
        testContactDTO = createTestContactDTO();
    }

    private Contact createTestContact() {
        Contact contact = new Contact();
        contact.setId(contactId);
        contact.setTenantId(tenantId);
        contact.setAccountId(accountId);
        contact.setFirstName("Jane");
        contact.setLastName("Smith");
        contact.setEmail("jane.smith@healthcare.com");
        contact.setPhone("555-1234");
        contact.setMobile("555-5678");
        contact.setTitle("Chief Medical Officer");
        contact.setDepartment("Executive");
        contact.setContactType(ContactType.DECISION_MAKER);
        contact.setPrimary(true);
        contact.setDoNotCall(false);
        contact.setDoNotEmail(false);
        contact.setLinkedinUrl("https://linkedin.com/in/janesmith");
        contact.setNotes("Key decision maker for quality initiatives");
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        return contact;
    }

    private ContactDTO createTestContactDTO() {
        return ContactDTO.builder()
            .id(contactId)
            .tenantId(tenantId)
            .accountId(accountId)
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@healthcare.com")
            .phone("555-1234")
            .mobile("555-5678")
            .title("Chief Medical Officer")
            .department("Executive")
            .contactType(ContactType.DECISION_MAKER)
            .isPrimary(true)
            .doNotCall(false)
            .doNotEmail(false)
            .linkedinUrl("https://linkedin.com/in/janesmith")
            .notes("Key decision maker for quality initiatives")
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return contact when found")
        void shouldReturnContactWhenFound() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));

            ContactDTO result = contactService.findById(tenantId, contactId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contactId);
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            assertThat(result.getEmail()).isEqualTo("jane.smith@healthcare.com");
            verify(contactRepository).findByIdAndTenantId(contactId, tenantId);
        }

        @Test
        @DisplayName("should throw RuntimeException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.findById(tenantId, contactId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contact not found");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated contacts")
        void shouldReturnPaginatedContacts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Contact> contactPage = new PageImpl<>(List.of(testContact), pageable, 1);

            when(contactRepository.findByTenantId(tenantId, pageable)).thenReturn(contactPage);

            Page<ContactDTO> result = contactService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new contact")
        void shouldCreateAndReturnNewContact() {
            when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

            ContactDTO result = contactService.create(tenantId, testContactDTO);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            verify(contactRepository).save(any(Contact.class));
        }

        @Test
        @DisplayName("should set default values for boolean fields")
        void shouldSetDefaultValuesForBooleanFields() {
            ContactDTO dtoWithNullBooleans = ContactDTO.builder()
                .firstName("New")
                .lastName("Contact")
                .email("new@test.com")
                .build();

            Contact savedContact = new Contact();
            savedContact.setId(UUID.randomUUID());
            savedContact.setTenantId(tenantId);
            savedContact.setFirstName("New");
            savedContact.setLastName("Contact");
            savedContact.setPrimary(false);
            savedContact.setDoNotCall(false);
            savedContact.setDoNotEmail(false);

            when(contactRepository.save(any(Contact.class))).thenReturn(savedContact);

            ContactDTO result = contactService.create(tenantId, dtoWithNullBooleans);

            assertThat(result).isNotNull();
            assertThat(result.getIsPrimary()).isFalse();
            assertThat(result.getDoNotCall()).isFalse();
            assertThat(result.getDoNotEmail()).isFalse();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing contact")
        void shouldUpdateExistingContact() {
            ContactDTO updateDTO = ContactDTO.builder()
                .firstName("Janet")
                .title("VP of Operations")
                .build();

            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));
            when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

            ContactDTO result = contactService.update(tenantId, contactId, updateDTO);

            assertThat(result).isNotNull();
            verify(contactRepository).save(testContact);
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            String originalLastName = testContact.getLastName();
            ContactDTO updateDTO = ContactDTO.builder()
                .firstName("Updated")
                .build();

            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

            contactService.update(tenantId, contactId, updateDTO);

            assertThat(testContact.getFirstName()).isEqualTo("Updated");
            assertThat(testContact.getLastName()).isEqualTo(originalLastName);
        }

        @Test
        @DisplayName("should throw RuntimeException when updating non-existent contact")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.update(tenantId, contactId, testContactDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contact not found");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing contact")
        void shouldDeleteExistingContact() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));

            contactService.delete(tenantId, contactId);

            verify(contactRepository).delete(testContact);
        }

        @Test
        @DisplayName("should throw RuntimeException when deleting non-existent contact")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactService.delete(tenantId, contactId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Contact not found");
        }
    }

    @Nested
    @DisplayName("findByAccount")
    class FindByAccount {

        @Test
        @DisplayName("should return contacts for specific account")
        void shouldReturnContactsForAccount() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Contact> contactPage = new PageImpl<>(List.of(testContact), pageable, 1);

            when(contactRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable))
                .thenReturn(contactPage);

            Page<ContactDTO> result = contactService.findByAccount(tenantId, accountId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAccountId()).isEqualTo(accountId);
        }
    }

    @Nested
    @DisplayName("findAllByAccount")
    class FindAllByAccount {

        @Test
        @DisplayName("should return all contacts for account without pagination")
        void shouldReturnAllContactsForAccount() {
            when(contactRepository.findByAccountId(accountId))
                .thenReturn(List.of(testContact));

            List<ContactDTO> result = contactService.findAllByAccount(accountId);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountId()).isEqualTo(accountId);
        }
    }

    @Nested
    @DisplayName("findPrimaryContact")
    class FindPrimaryContact {

        @Test
        @DisplayName("should return primary contact when exists")
        void shouldReturnPrimaryContactWhenExists() {
            when(contactRepository.findPrimaryContactByAccountId(accountId))
                .thenReturn(Optional.of(testContact));

            ContactDTO result = contactService.findPrimaryContact(accountId);

            assertThat(result).isNotNull();
            assertThat(result.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("should return null when no primary contact exists")
        void shouldReturnNullWhenNoPrimaryContact() {
            when(contactRepository.findPrimaryContactByAccountId(accountId))
                .thenReturn(Optional.empty());

            ContactDTO result = contactService.findPrimaryContact(accountId);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByContactType")
    class FindByContactType {

        @Test
        @DisplayName("should return contacts filtered by type")
        void shouldReturnContactsFilteredByType() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Contact> contactPage = new PageImpl<>(List.of(testContact), pageable, 1);

            when(contactRepository.findByTenantIdAndContactType(tenantId, ContactType.DECISION_MAKER, pageable))
                .thenReturn(contactPage);

            Page<ContactDTO> result = contactService.findByContactType(tenantId, ContactType.DECISION_MAKER, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContactType()).isEqualTo(ContactType.DECISION_MAKER);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should return contacts matching search query")
        void shouldReturnContactsMatchingSearchQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Contact> contactPage = new PageImpl<>(List.of(testContact), pageable, 1);

            when(contactRepository.searchContacts(tenantId, "Jane", pageable))
                .thenReturn(contactPage);

            Page<ContactDTO> result = contactService.search(tenantId, "Jane", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findContactableContacts")
    class FindContactableContacts {

        @Test
        @DisplayName("should return only contactable contacts")
        void shouldReturnOnlyContactableContacts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Contact> contactPage = new PageImpl<>(List.of(testContact), pageable, 1);

            when(contactRepository.findContactableContacts(tenantId, pageable))
                .thenReturn(contactPage);

            Page<ContactDTO> result = contactService.findContactableContacts(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDoNotCall()).isFalse();
            assertThat(result.getContent().get(0).getDoNotEmail()).isFalse();
        }
    }

    @Nested
    @DisplayName("setPrimaryContact")
    class SetPrimaryContact {

        @Test
        @DisplayName("should set contact as primary")
        void shouldSetContactAsPrimary() {
            Contact nonPrimaryContact = new Contact();
            nonPrimaryContact.setId(contactId);
            nonPrimaryContact.setTenantId(tenantId);
            nonPrimaryContact.setAccountId(accountId);
            nonPrimaryContact.setPrimary(false);

            Contact existingPrimary = new Contact();
            existingPrimary.setId(UUID.randomUUID());
            existingPrimary.setTenantId(tenantId);
            existingPrimary.setAccountId(accountId);
            existingPrimary.setPrimary(true);

            when(contactRepository.findByAccountId(accountId))
                .thenReturn(List.of(existingPrimary));
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(nonPrimaryContact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

            ContactDTO result = contactService.setPrimaryContact(tenantId, accountId, contactId);

            assertThat(result).isNotNull();
            assertThat(result.getIsPrimary()).isTrue();
            verify(contactRepository, atLeast(2)).save(any(Contact.class));
        }

        @Test
        @DisplayName("should throw exception when contact does not belong to account")
        void shouldThrowExceptionWhenContactNotInAccount() {
            Contact contactInDifferentAccount = new Contact();
            contactInDifferentAccount.setId(contactId);
            contactInDifferentAccount.setTenantId(tenantId);
            contactInDifferentAccount.setAccountId(UUID.randomUUID()); // Different account

            when(contactRepository.findByAccountId(accountId))
                .thenReturn(List.of());
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(contactInDifferentAccount));

            assertThatThrownBy(() -> contactService.setPrimaryContact(tenantId, accountId, contactId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to the specified account");
        }
    }

    @Nested
    @DisplayName("recordContactActivity")
    class RecordContactActivity {

        @Test
        @DisplayName("should update last contacted timestamp")
        void shouldUpdateLastContactedTimestamp() {
            LocalDateTime beforeUpdate = testContact.getLastContactedAt();

            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

            ContactDTO result = contactService.recordContactActivity(tenantId, contactId);

            assertThat(result).isNotNull();
            assertThat(testContact.getLastContactedAt()).isNotNull();
            assertThat(testContact.getLastContactedAt()).isAfterOrEqualTo(LocalDateTime.now().minusSeconds(5));
            verify(contactRepository).save(testContact);
        }
    }

    @Nested
    @DisplayName("DTO Mapping")
    class DTOMapping {

        @Test
        @DisplayName("should correctly map all fields from entity to DTO")
        void shouldCorrectlyMapAllFieldsFromEntityToDTO() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));

            ContactDTO result = contactService.findById(tenantId, contactId);

            assertThat(result.getId()).isEqualTo(testContact.getId());
            assertThat(result.getTenantId()).isEqualTo(testContact.getTenantId());
            assertThat(result.getAccountId()).isEqualTo(testContact.getAccountId());
            assertThat(result.getFirstName()).isEqualTo(testContact.getFirstName());
            assertThat(result.getLastName()).isEqualTo(testContact.getLastName());
            assertThat(result.getEmail()).isEqualTo(testContact.getEmail());
            assertThat(result.getPhone()).isEqualTo(testContact.getPhone());
            assertThat(result.getMobile()).isEqualTo(testContact.getMobile());
            assertThat(result.getTitle()).isEqualTo(testContact.getTitle());
            assertThat(result.getDepartment()).isEqualTo(testContact.getDepartment());
            assertThat(result.getContactType()).isEqualTo(testContact.getContactType());
            assertThat(result.getIsPrimary()).isEqualTo(testContact.getPrimary());
            assertThat(result.getDoNotCall()).isEqualTo(testContact.getDoNotCall());
            assertThat(result.getDoNotEmail()).isEqualTo(testContact.getDoNotEmail());
            assertThat(result.getLinkedinUrl()).isEqualTo(testContact.getLinkedinUrl());
            assertThat(result.getNotes()).isEqualTo(testContact.getNotes());
        }
    }
}
