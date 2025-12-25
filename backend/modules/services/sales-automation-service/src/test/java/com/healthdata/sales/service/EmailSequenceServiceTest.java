package com.healthdata.sales.service;

import com.healthdata.sales.dto.EmailSequenceDTO;
import com.healthdata.sales.dto.SequenceEnrollmentDTO;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.exception.*;
import com.healthdata.sales.repository.*;
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

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSequenceServiceTest {

    @Mock
    private EmailSequenceRepository sequenceRepository;

    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private EmailSequenceService emailSequenceService;

    private UUID tenantId;
    private UUID sequenceId;
    private UUID leadId;
    private UUID contactId;
    private EmailSequence testSequence;
    private Lead testLead;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        sequenceId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        contactId = UUID.randomUUID();

        testSequence = createTestSequence();
        testLead = createTestLead();
        testContact = createTestContact();
    }

    private EmailSequence createTestSequence() {
        EmailSequence sequence = new EmailSequence();
        sequence.setId(sequenceId);
        sequence.setTenantId(tenantId);
        sequence.setName("Welcome Sequence");
        sequence.setDescription("Onboarding email sequence");
        sequence.setSequenceType(SequenceType.WELCOME);
        sequence.setTargetType(TargetType.LEAD);
        sequence.setActive(true);
        sequence.setFromName("Sales Team");
        sequence.setFromEmail("sales@example.com");
        sequence.setSteps(new ArrayList<>());
        return sequence;
    }

    private Lead createTestLead() {
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId(tenantId);
        lead.setFirstName("John");
        lead.setLastName("Doe");
        lead.setEmail("john.doe@example.com");
        return lead;
    }

    private Contact createTestContact() {
        Contact contact = new Contact();
        contact.setId(contactId);
        contact.setTenantId(tenantId);
        contact.setFirstName("Jane");
        contact.setLastName("Smith");
        contact.setEmail("jane.smith@example.com");
        return contact;
    }

    @Nested
    @DisplayName("findSequenceById")
    class FindSequenceById {

        @Test
        @DisplayName("should return sequence when found")
        void shouldReturnSequenceWhenFound() {
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.countBySequenceId(sequenceId)).thenReturn(10L);
            when(enrollmentRepository.countBySequenceIdAndStatus(sequenceId, EnrollmentStatus.ACTIVE))
                .thenReturn(5L);

            EmailSequenceDTO result = emailSequenceService.findSequenceById(tenantId, sequenceId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Welcome Sequence");
            assertThat(result.getTotalEnrollments()).isEqualTo(10L);
            assertThat(result.getActiveEnrollments()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should throw SequenceNotFoundException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailSequenceService.findSequenceById(tenantId, sequenceId))
                .isInstanceOf(SequenceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createSequence")
    class CreateSequence {

        @Test
        @DisplayName("should create sequence successfully")
        void shouldCreateSequenceSuccessfully() {
            EmailSequenceDTO dto = EmailSequenceDTO.builder()
                .name("New Sequence")
                .description("A new sequence")
                .sequenceType(SequenceType.NURTURE)
                .targetType(TargetType.LEAD)
                .fromName("Marketing")
                .fromEmail("marketing@example.com")
                .build();

            when(sequenceRepository.existsByNameAndTenantId("New Sequence", tenantId))
                .thenReturn(false);
            when(sequenceRepository.save(any(EmailSequence.class)))
                .thenAnswer(inv -> {
                    EmailSequence saved = inv.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });
            when(enrollmentRepository.countBySequenceId(any())).thenReturn(0L);
            when(enrollmentRepository.countBySequenceIdAndStatus(any(), any())).thenReturn(0L);

            EmailSequenceDTO result = emailSequenceService.createSequence(tenantId, dto);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Sequence");
            verify(sequenceRepository).save(any(EmailSequence.class));
        }

        @Test
        @DisplayName("should throw exception when name already exists")
        void shouldThrowExceptionWhenNameExists() {
            EmailSequenceDTO dto = EmailSequenceDTO.builder()
                .name("Existing Sequence")
                .build();

            when(sequenceRepository.existsByNameAndTenantId("Existing Sequence", tenantId))
                .thenReturn(true);

            assertThatThrownBy(() -> emailSequenceService.createSequence(tenantId, dto))
                .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("deleteSequence")
    class DeleteSequence {

        @Test
        @DisplayName("should delete sequence with no active enrollments")
        void shouldDeleteSequenceWithNoActiveEnrollments() {
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.countBySequenceIdAndStatus(sequenceId, EnrollmentStatus.ACTIVE))
                .thenReturn(0L);

            emailSequenceService.deleteSequence(tenantId, sequenceId);

            verify(sequenceRepository).delete(testSequence);
        }

        @Test
        @DisplayName("should throw exception when sequence has active enrollments")
        void shouldThrowExceptionWhenHasActiveEnrollments() {
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.countBySequenceIdAndStatus(sequenceId, EnrollmentStatus.ACTIVE))
                .thenReturn(5L);

            assertThatThrownBy(() -> emailSequenceService.deleteSequence(tenantId, sequenceId))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("5 active enrollments");
        }
    }

    @Nested
    @DisplayName("enrollLead")
    class EnrollLead {

        @Test
        @DisplayName("should enroll lead successfully")
        void shouldEnrollLeadSuccessfully() {
            UUID enrolledByUserId = UUID.randomUUID();

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.findExistingEnrollment(sequenceId, leadId, null))
                .thenReturn(Optional.empty());
            when(enrollmentRepository.save(any(SequenceEnrollment.class)))
                .thenAnswer(inv -> {
                    SequenceEnrollment saved = inv.getArgument(0);
                    saved.setSequence(testSequence);
                    return saved;
                });

            SequenceEnrollmentDTO result = emailSequenceService.enrollLead(
                tenantId, leadId, sequenceId, enrolledByUserId);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(enrollmentRepository).save(any(SequenceEnrollment.class));
        }

        @Test
        @DisplayName("should throw exception when lead not found")
        void shouldThrowExceptionWhenLeadNotFound() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                tenantId, leadId, sequenceId, UUID.randomUUID()))
                .isInstanceOf(LeadNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when sequence is inactive")
        void shouldThrowExceptionWhenSequenceInactive() {
            testSequence.setActive(false);

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));

            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                tenantId, leadId, sequenceId, UUID.randomUUID()))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("should throw exception when sequence is for contacts only")
        void shouldThrowExceptionWhenSequenceForContactsOnly() {
            testSequence.setTargetType(TargetType.CONTACT);

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));

            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                tenantId, leadId, sequenceId, UUID.randomUUID()))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("contacts only");
        }

        @Test
        @DisplayName("should throw exception when lead already enrolled")
        void shouldThrowExceptionWhenAlreadyEnrolled() {
            SequenceEnrollment existing = new SequenceEnrollment();
            existing.setId(UUID.randomUUID());

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.findExistingEnrollment(sequenceId, leadId, null))
                .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> emailSequenceService.enrollLead(
                tenantId, leadId, sequenceId, UUID.randomUUID()))
                .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("enrollContact")
    class EnrollContact {

        @Test
        @DisplayName("should enroll contact successfully")
        void shouldEnrollContactSuccessfully() {
            testSequence.setTargetType(TargetType.CONTACT);
            UUID enrolledByUserId = UUID.randomUUID();

            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.of(testContact));
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(enrollmentRepository.findExistingEnrollment(sequenceId, null, contactId))
                .thenReturn(Optional.empty());
            when(enrollmentRepository.save(any(SequenceEnrollment.class)))
                .thenAnswer(inv -> {
                    SequenceEnrollment saved = inv.getArgument(0);
                    saved.setSequence(testSequence);
                    return saved;
                });

            SequenceEnrollmentDTO result = emailSequenceService.enrollContact(
                tenantId, contactId, sequenceId, enrolledByUserId);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("jane.smith@example.com");
        }

        @Test
        @DisplayName("should throw exception when contact not found")
        void shouldThrowExceptionWhenContactNotFound() {
            when(contactRepository.findByIdAndTenantId(contactId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailSequenceService.enrollContact(
                tenantId, contactId, sequenceId, UUID.randomUUID()))
                .isInstanceOf(ContactNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activateSequence")
    class ActivateSequence {

        @Test
        @DisplayName("should activate sequence")
        void shouldActivateSequence() {
            testSequence.setActive(false);

            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(any(EmailSequence.class))).thenReturn(testSequence);
            when(enrollmentRepository.countBySequenceId(any())).thenReturn(0L);
            when(enrollmentRepository.countBySequenceIdAndStatus(any(), any())).thenReturn(0L);

            EmailSequenceDTO result = emailSequenceService.activateSequence(tenantId, sequenceId);

            assertThat(result).isNotNull();
            verify(sequenceRepository).save(argThat(seq -> seq.getActive()));
        }
    }

    @Nested
    @DisplayName("deactivateSequence")
    class DeactivateSequence {

        @Test
        @DisplayName("should deactivate sequence")
        void shouldDeactivateSequence() {
            when(sequenceRepository.findByIdAndTenantId(sequenceId, tenantId))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(any(EmailSequence.class))).thenReturn(testSequence);
            when(enrollmentRepository.countBySequenceId(any())).thenReturn(0L);
            when(enrollmentRepository.countBySequenceIdAndStatus(any(), any())).thenReturn(0L);

            EmailSequenceDTO result = emailSequenceService.deactivateSequence(tenantId, sequenceId);

            assertThat(result).isNotNull();
            verify(sequenceRepository).save(argThat(seq -> !seq.getActive()));
        }
    }
}
