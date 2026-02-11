package com.healthdata.sales.service;

import com.healthdata.sales.config.LinkedInConfig;
import com.healthdata.sales.dto.LinkedInBulkCampaignResponse;
import com.healthdata.sales.dto.LinkedInOutreachDTO;
import com.healthdata.sales.entity.Contact;
import com.healthdata.sales.entity.Lead;
import com.healthdata.sales.entity.LinkedInOutreach;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
import com.healthdata.sales.exception.DuplicateResourceException;
import com.healthdata.sales.repository.ContactRepository;
import com.healthdata.sales.repository.LeadRepository;
import com.healthdata.sales.repository.LinkedInOutreachRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LinkedInOutreachService.
 *
 * Tests cover:
 * - CRUD operations (findAll, findById, findByStatus, findByCampaign)
 * - Connection request scheduling (for leads and contacts)
 * - InMail scheduling
 * - Bulk campaign creation
 * - Status update methods (markAsSent, markAsAccepted, markAsReplied, cancel)
 * - Analytics calculations
 * - Daily limit enforcement
 * - Message personalization
 * - Multi-tenant isolation
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LinkedInOutreachService Unit Tests")
class LinkedInOutreachServiceTest {

    @Mock
    private LinkedInOutreachRepository outreachRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private LinkedInConfig linkedInConfig;

    @InjectMocks
    private LinkedInOutreachService outreachService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID LEAD_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID OUTREACH_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String LINKEDIN_URL = "https://linkedin.com/in/john-doe";

    private Lead testLead;
    private Contact testContact;
    private LinkedInOutreach testOutreach;
    private LinkedInConfig.Outreach outreachConfig;

    @BeforeEach
    void setUp() {
        testLead = Lead.builder()
            .id(LEAD_ID)
            .tenantId(TENANT_ID)
            .firstName("John")
            .lastName("Doe")
            .title("VP of Sales")
            .company("Acme Corp")
            .linkedinUrl(LINKEDIN_URL)
            .build();

        testContact = Contact.builder()
            .id(CONTACT_ID)
            .tenantId(TENANT_ID)
            .firstName("Jane")
            .lastName("Smith")
            .title("Director of Operations")
            .linkedinUrl(LINKEDIN_URL)
            .build();

        testOutreach = LinkedInOutreach.builder()
            .id(OUTREACH_ID)
            .tenantId(TENANT_ID)
            .leadId(LEAD_ID)
            .linkedinProfileUrl(LINKEDIN_URL)
            .targetName("John Doe")
            .targetTitle("VP of Sales")
            .targetCompany("Acme Corp")
            .outreachType(OutreachType.CONNECTION_REQUEST)
            .status(OutreachStatus.PENDING)
            .campaignName("Test Campaign")
            .scheduledAt(LocalDateTime.now())
            .createdBy(USER_ID)
            .build();

        outreachConfig = new LinkedInConfig.Outreach();
        outreachConfig.setEnabled(true);
        outreachConfig.setMaxConnectionsPerDay(50);
        outreachConfig.setMaxInMailsPerDay(25);
    }

    // ==========================================
    // CRUD Operations Tests
    // ==========================================

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("should return paginated outreach list for tenant")
        void shouldReturnPaginatedOutreachList() {
            Pageable pageable = PageRequest.of(0, 10);
            List<LinkedInOutreach> outreachList = List.of(testOutreach);
            Page<LinkedInOutreach> page = new PageImpl<>(outreachList, pageable, 1);

            when(outreachRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            Page<LinkedInOutreachDTO> result = outreachService.findAll(TENANT_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTargetName()).isEqualTo("John Doe");
            verify(outreachRepository).findByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should return empty page when no outreach exists")
        void shouldReturnEmptyPageWhenNoOutreach() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInOutreach> emptyPage = Page.empty(pageable);

            when(outreachRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

            Page<LinkedInOutreachDTO> result = outreachService.findAll(TENANT_ID, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return outreach when found")
        void shouldReturnOutreachWhenFound() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.of(testOutreach));

            Optional<LinkedInOutreachDTO> result = outreachService.findById(TENANT_ID, OUTREACH_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(OUTREACH_ID);
            assertThat(result.get().getTargetName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should return empty when outreach not found")
        void shouldReturnEmptyWhenNotFound() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            Optional<LinkedInOutreachDTO> result = outreachService.findById(TENANT_ID, OUTREACH_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should enforce tenant isolation")
        void shouldEnforceTenantIsolation() {
            UUID otherTenantId = UUID.randomUUID();
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, otherTenantId))
                .thenReturn(Optional.empty());

            Optional<LinkedInOutreachDTO> result = outreachService.findById(otherTenantId, OUTREACH_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus Tests")
    class FindByStatusTests {

        @Test
        @DisplayName("should return outreach filtered by status")
        void shouldReturnOutreachFilteredByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInOutreach> page = new PageImpl<>(List.of(testOutreach), pageable, 1);

            when(outreachRepository.findByTenantIdAndStatus(TENANT_ID, OutreachStatus.PENDING, pageable))
                .thenReturn(page);

            Page<LinkedInOutreachDTO> result = outreachService.findByStatus(TENANT_ID, OutreachStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(OutreachStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("findByCampaign Tests")
    class FindByCampaignTests {

        @Test
        @DisplayName("should return outreach filtered by campaign name")
        void shouldReturnOutreachFilteredByCampaign() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<LinkedInOutreach> page = new PageImpl<>(List.of(testOutreach), pageable, 1);

            when(outreachRepository.findByTenantIdAndCampaignName(TENANT_ID, "Test Campaign", pageable))
                .thenReturn(page);

            Page<LinkedInOutreachDTO> result = outreachService.findByCampaign(TENANT_ID, "Test Campaign", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCampaignName()).isEqualTo("Test Campaign");
        }
    }

    // ==========================================
    // Connection Request Tests
    // ==========================================

    @Nested
    @DisplayName("scheduleConnectionRequest Tests")
    class ScheduleConnectionRequestTests {

        @BeforeEach
        void setUpConfig() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
        }

        @Test
        @DisplayName("should schedule connection request for lead")
        void shouldScheduleConnectionRequestForLead() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(eq(TENANT_ID), eq(LINKEDIN_URL), eq(OutreachType.CONNECTION_REQUEST)))
                .thenReturn(false);
            when(outreachRepository.countDailyOutreach(eq(TENANT_ID), eq(OutreachType.CONNECTION_REQUEST), any(), any()))
                .thenReturn(0L);
            when(outreachRepository.save(any(LinkedInOutreach.class))).thenAnswer(invocation -> {
                LinkedInOutreach saved = invocation.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            LinkedInOutreachDTO result = outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID, "Hi {{firstName}}!", "Test Campaign", null, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getTargetName()).isEqualTo("John Doe");
            assertThat(result.getOutreachType()).isEqualTo(OutreachType.CONNECTION_REQUEST);
            assertThat(result.getStatus()).isEqualTo(OutreachStatus.PENDING);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getConnectionNote()).isEqualTo("Hi John!");
        }

        @Test
        @DisplayName("should throw exception when lead not found")
        void shouldThrowExceptionWhenLeadNotFound() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID, "Hi!", "Campaign", null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lead not found");
        }

        @Test
        @DisplayName("should throw exception when lead has no LinkedIn URL")
        void shouldThrowExceptionWhenNoLinkedInUrl() {
            testLead.setLinkedinUrl(null);
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));

            assertThatThrownBy(() -> outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID, "Hi!", "Campaign", null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LinkedIn profile URL");
        }

        @Test
        @DisplayName("should throw exception for duplicate connection request")
        void shouldThrowExceptionForDuplicateRequest() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(eq(TENANT_ID), eq(LINKEDIN_URL), eq(OutreachType.CONNECTION_REQUEST)))
                .thenReturn(true);

            assertThatThrownBy(() -> outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID, "Hi!", "Campaign", null, USER_ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("should throw exception when daily limit reached")
        void shouldThrowExceptionWhenDailyLimitReached() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(eq(TENANT_ID), eq(LINKEDIN_URL), eq(OutreachType.CONNECTION_REQUEST)))
                .thenReturn(false);
            when(outreachRepository.countDailyOutreach(eq(TENANT_ID), eq(OutreachType.CONNECTION_REQUEST), any(), any()))
                .thenReturn(50L); // At limit

            assertThatThrownBy(() -> outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID, "Hi!", "Campaign", null, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Daily");
        }

        @Test
        @DisplayName("should truncate connection note to 300 characters")
        void shouldTruncateConnectionNoteTo300Chars() {
            String longNote = "A".repeat(400);
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            outreachService.scheduleConnectionRequest(TENANT_ID, LEAD_ID, longNote, "Campaign", null, USER_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getConnectionNote()).hasSize(300);
            assertThat(captor.getValue().getConnectionNote()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("scheduleConnectionRequestForContact Tests")
    class ScheduleConnectionRequestForContactTests {

        @BeforeEach
        void setUpConfig() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
        }

        @Test
        @DisplayName("should schedule connection request for contact")
        void shouldScheduleConnectionRequestForContact() {
            when(contactRepository.findByIdAndTenantId(CONTACT_ID, TENANT_ID)).thenReturn(Optional.of(testContact));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            LinkedInOutreachDTO result = outreachService.scheduleConnectionRequestForContact(
                TENANT_ID, CONTACT_ID, "Hi {{firstName}}!", "Contact Campaign", null, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getTargetName()).isEqualTo("Jane Smith");
            assertThat(result.getContactId()).isEqualTo(CONTACT_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getConnectionNote()).isEqualTo("Hi Jane!");
        }

        @Test
        @DisplayName("should throw exception when contact not found")
        void shouldThrowExceptionWhenContactNotFound() {
            when(contactRepository.findByIdAndTenantId(CONTACT_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outreachService.scheduleConnectionRequestForContact(
                TENANT_ID, CONTACT_ID, "Hi!", "Campaign", null, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contact not found");
        }
    }

    // ==========================================
    // InMail Tests
    // ==========================================

    @Nested
    @DisplayName("scheduleInMail Tests")
    class ScheduleInMailTests {

        @BeforeEach
        void setUpConfig() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
        }

        @Test
        @DisplayName("should schedule InMail for lead")
        void shouldScheduleInMailForLead() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.countDailyOutreach(eq(TENANT_ID), eq(OutreachType.INMAIL), any(), any()))
                .thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            LinkedInOutreachDTO result = outreachService.scheduleInMail(
                TENANT_ID, LEAD_ID, "Important Opportunity",
                "Hi {{firstName}}, I wanted to reach out...", "InMail Campaign", null, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getOutreachType()).isEqualTo(OutreachType.INMAIL);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getMessageContent()).contains("Hi John");
        }

        @Test
        @DisplayName("should throw exception when InMail daily limit reached")
        void shouldThrowExceptionWhenInMailLimitReached() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.countDailyOutreach(eq(TENANT_ID), eq(OutreachType.INMAIL), any(), any()))
                .thenReturn(25L); // At limit

            assertThatThrownBy(() -> outreachService.scheduleInMail(
                TENANT_ID, LEAD_ID, "Subject", "Message", "Campaign", null, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Daily");
        }
    }

    // ==========================================
    // Bulk Campaign Tests
    // ==========================================

    @Nested
    @DisplayName("createBulkCampaign Tests")
    class CreateBulkCampaignTests {

        @BeforeEach
        void setUpConfig() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
        }

        @Test
        @DisplayName("should create bulk campaign for multiple leads")
        void shouldCreateBulkCampaignForMultipleLeads() {
            UUID lead1Id = UUID.randomUUID();
            UUID lead2Id = UUID.randomUUID();

            Lead lead1 = Lead.builder()
                .id(lead1Id)
                .tenantId(TENANT_ID)
                .firstName("Alice")
                .lastName("Johnson")
                .linkedinUrl("https://linkedin.com/in/alice")
                .build();

            Lead lead2 = Lead.builder()
                .id(lead2Id)
                .tenantId(TENANT_ID)
                .firstName("Bob")
                .lastName("Williams")
                .linkedinUrl("https://linkedin.com/in/bob")
                .build();

            when(leadRepository.findByIdAndTenantId(lead1Id, TENANT_ID)).thenReturn(Optional.of(lead1));
            when(leadRepository.findByIdAndTenantId(lead2Id, TENANT_ID)).thenReturn(Optional.of(lead2));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            LinkedInBulkCampaignResponse result = outreachService.createBulkCampaign(
                TENANT_ID, "Bulk Campaign", List.of(lead1Id, lead2Id),
                OutreachType.CONNECTION_REQUEST, "Hi {{firstName}}!",
                LocalDateTime.now(), 30, USER_ID);

            assertThat(result.getTotalLeads()).isEqualTo(2);
            assertThat(result.getScheduled()).isEqualTo(2);
            assertThat(result.getErrors()).isEmpty();
            verify(outreachRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("should track errors for leads without LinkedIn URL")
        void shouldTrackErrorsForLeadsWithoutLinkedInUrl() {
            Lead leadWithoutUrl = Lead.builder()
                .id(LEAD_ID)
                .tenantId(TENANT_ID)
                .firstName("NoUrl")
                .lastName("User")
                .linkedinUrl(null)
                .build();

            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(leadWithoutUrl));

            LinkedInBulkCampaignResponse result = outreachService.createBulkCampaign(
                TENANT_ID, "Bulk Campaign", List.of(LEAD_ID),
                OutreachType.CONNECTION_REQUEST, "Hi!", null, 30, USER_ID);

            assertThat(result.getTotalLeads()).isEqualTo(1);
            assertThat(result.getScheduled()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("No LinkedIn URL");
        }

        @Test
        @DisplayName("should skip already contacted leads")
        void shouldSkipAlreadyContactedLeads() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(eq(TENANT_ID), eq(LINKEDIN_URL), eq(OutreachType.CONNECTION_REQUEST)))
                .thenReturn(true);

            LinkedInBulkCampaignResponse result = outreachService.createBulkCampaign(
                TENANT_ID, "Bulk Campaign", List.of(LEAD_ID),
                OutreachType.CONNECTION_REQUEST, "Hi!", null, 30, USER_ID);

            assertThat(result.getScheduled()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Already contacted");
        }

        @Test
        @DisplayName("should schedule with incremental delays")
        void shouldScheduleWithIncrementalDelays() {
            UUID lead1Id = UUID.randomUUID();
            UUID lead2Id = UUID.randomUUID();

            Lead lead1 = Lead.builder()
                .id(lead1Id).tenantId(TENANT_ID).firstName("A").lastName("A")
                .linkedinUrl("https://linkedin.com/in/a").build();
            Lead lead2 = Lead.builder()
                .id(lead2Id).tenantId(TENANT_ID).firstName("B").lastName("B")
                .linkedinUrl("https://linkedin.com/in/b").build();

            when(leadRepository.findByIdAndTenantId(lead1Id, TENANT_ID)).thenReturn(Optional.of(lead1));
            when(leadRepository.findByIdAndTenantId(lead2Id, TENANT_ID)).thenReturn(Optional.of(lead2));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);

            List<LinkedInOutreach> savedOutreach = new ArrayList<>();
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(UUID.randomUUID());
                savedOutreach.add(saved);
                return saved;
            });

            LocalDateTime startDate = LocalDateTime.of(2026, 2, 4, 10, 0);
            outreachService.createBulkCampaign(
                TENANT_ID, "Campaign", List.of(lead1Id, lead2Id),
                OutreachType.CONNECTION_REQUEST, "Hi!", startDate, 60, USER_ID);

            assertThat(savedOutreach).hasSize(2);
            assertThat(savedOutreach.get(0).getScheduledAt()).isEqualTo(startDate);
            assertThat(savedOutreach.get(1).getScheduledAt()).isEqualTo(startDate.plusMinutes(60));
        }
    }

    // ==========================================
    // Status Update Tests
    // ==========================================

    @Nested
    @DisplayName("Status Update Tests")
    class StatusUpdateTests {

        @Test
        @DisplayName("should mark outreach as sent")
        void shouldMarkOutreachAsSent() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.of(testOutreach));
            when(outreachRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LinkedInOutreachDTO result = outreachService.markAsSent(TENANT_ID, OUTREACH_ID);

            assertThat(result.getStatus()).isEqualTo(OutreachStatus.SENT);
            assertThat(testOutreach.getSentAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception when marking non-existent outreach as sent")
        void shouldThrowExceptionWhenMarkingNonExistentAsSent() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> outreachService.markAsSent(TENANT_ID, OUTREACH_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Outreach not found");
        }

        @Test
        @DisplayName("should mark outreach as accepted")
        void shouldMarkOutreachAsAccepted() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.of(testOutreach));
            when(outreachRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LinkedInOutreachDTO result = outreachService.markAsAccepted(TENANT_ID, OUTREACH_ID);

            assertThat(result.getStatus()).isEqualTo(OutreachStatus.ACCEPTED);
            assertThat(result.getConnectionAccepted()).isTrue();
        }

        @Test
        @DisplayName("should mark outreach as replied")
        void shouldMarkOutreachAsReplied() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.of(testOutreach));
            when(outreachRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LinkedInOutreachDTO result = outreachService.markAsReplied(TENANT_ID, OUTREACH_ID);

            assertThat(result.getStatus()).isEqualTo(OutreachStatus.REPLIED);
            assertThat(result.getReplied()).isTrue();
        }

        @Test
        @DisplayName("should cancel outreach")
        void shouldCancelOutreach() {
            when(outreachRepository.findByIdAndTenantId(OUTREACH_ID, TENANT_ID))
                .thenReturn(Optional.of(testOutreach));
            when(outreachRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LinkedInOutreachDTO result = outreachService.cancel(TENANT_ID, OUTREACH_ID);

            assertThat(result.getStatus()).isEqualTo(OutreachStatus.CANCELLED);
        }
    }

    // ==========================================
    // Analytics Tests
    // ==========================================

    @Nested
    @DisplayName("getAnalytics Tests")
    class GetAnalyticsTests {

        @Test
        @DisplayName("should calculate analytics correctly")
        void shouldCalculateAnalyticsCorrectly() {
            when(outreachRepository.countSentSince(eq(TENANT_ID), eq(OutreachType.CONNECTION_REQUEST), any()))
                .thenReturn(100L);
            when(outreachRepository.countAcceptedSince(eq(TENANT_ID), any()))
                .thenReturn(30L);
            when(outreachRepository.countRepliedSince(eq(TENANT_ID), any()))
                .thenReturn(10L);
            when(outreachRepository.countSentSince(eq(TENANT_ID), eq(OutreachType.INMAIL), any()))
                .thenReturn(50L);
            when(outreachRepository.countByStatus(TENANT_ID))
                .thenReturn(List.of(
                    new Object[]{OutreachStatus.PENDING, 20L},
                    new Object[]{OutreachStatus.SENT, 80L}
                ));

            LinkedInOutreachService.LinkedInAnalytics result = outreachService.getAnalytics(TENANT_ID, 30);

            assertThat(result.getPeriodDays()).isEqualTo(30);
            assertThat(result.getConnectionRequestsSent()).isEqualTo(100);
            assertThat(result.getConnectionsAccepted()).isEqualTo(30);
            assertThat(result.getInmailsSent()).isEqualTo(50);
            assertThat(result.getTotalReplies()).isEqualTo(10);
            assertThat(result.getAcceptanceRate()).isEqualTo(30.0); // 30/100 * 100
            assertThat(result.getReplyRate()).isCloseTo(6.67, org.assertj.core.api.Assertions.within(0.1)); // 10/150 * 100
            assertThat(result.getStatusBreakdown()).containsEntry("PENDING", 20L);
            assertThat(result.getStatusBreakdown()).containsEntry("SENT", 80L);
        }

        @Test
        @DisplayName("should handle zero sent for rate calculations")
        void shouldHandleZeroSentForRateCalculations() {
            when(outreachRepository.countSentSince(any(), any(), any())).thenReturn(0L);
            when(outreachRepository.countAcceptedSince(any(), any())).thenReturn(0L);
            when(outreachRepository.countRepliedSince(any(), any())).thenReturn(0L);
            when(outreachRepository.countByStatus(TENANT_ID)).thenReturn(List.of());

            LinkedInOutreachService.LinkedInAnalytics result = outreachService.getAnalytics(TENANT_ID, 7);

            assertThat(result.getAcceptanceRate()).isEqualTo(0.0);
            assertThat(result.getReplyRate()).isEqualTo(0.0);
        }
    }

    // ==========================================
    // Scheduled Task Tests
    // ==========================================

    @Nested
    @DisplayName("processScheduledOutreach Tests")
    class ProcessScheduledOutreachTests {

        @Test
        @DisplayName("should process due outreach when enabled")
        void shouldProcessDueOutreachWhenEnabled() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
            when(outreachRepository.findDueForSending(any()))
                .thenReturn(List.of(testOutreach));

            outreachService.processScheduledOutreach();

            verify(outreachRepository).findDueForSending(any());
        }

        @Test
        @DisplayName("should skip processing when disabled")
        void shouldSkipProcessingWhenDisabled() {
            outreachConfig.setEnabled(false);
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);

            outreachService.processScheduledOutreach();

            verify(outreachRepository, never()).findDueForSending(any());
        }

        @Test
        @DisplayName("should handle empty due list")
        void shouldHandleEmptyDueList() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
            when(outreachRepository.findDueForSending(any())).thenReturn(List.of());

            outreachService.processScheduledOutreach();

            verify(outreachRepository).findDueForSending(any());
        }
    }

    // ==========================================
    // Message Personalization Tests
    // ==========================================

    @Nested
    @DisplayName("Message Personalization Tests")
    class MessagePersonalizationTests {

        @BeforeEach
        void setUpConfig() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
        }

        @Test
        @DisplayName("should personalize message with merge fields")
        void shouldPersonalizeMessageWithMergeFields() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID,
                "Hi {{firstName}} {{lastName}} at {{company}}! Your role as {{title}} caught my attention.",
                "Campaign", null, USER_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            String note = captor.getValue().getConnectionNote();
            assertThat(note).contains("Hi John Doe at Acme Corp");
            assertThat(note).contains("VP of Sales");
        }

        @Test
        @DisplayName("should handle null values in merge fields")
        void shouldHandleNullValuesInMergeFields() {
            testLead.setCompany(null);
            testLead.setTitle(null);
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID,
                "Hi {{firstName}}! Working at {{company}} as {{title}}.",
                "Campaign", null, USER_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            String note = captor.getValue().getConnectionNote();
            assertThat(note).isEqualTo("Hi John! Working at  as .");
        }

        @Test
        @DisplayName("should preserve unknown merge fields")
        void shouldPreserveUnknownMergeFields() {
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            outreachService.scheduleConnectionRequest(
                TENANT_ID, LEAD_ID,
                "Hi {{firstName}}! Check out {{unknownField}}.",
                "Campaign", null, USER_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getConnectionNote()).contains("{{unknownField}}");
        }
    }

    // ==========================================
    // Multi-Tenant Isolation Tests
    // ==========================================

    @Nested
    @DisplayName("Multi-Tenant Isolation Tests")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("should isolate outreach by tenant in findAll")
        void shouldIsolateOutreachByTenantInFindAll() {
            UUID otherTenantId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            when(outreachRepository.findByTenantId(otherTenantId, pageable))
                .thenReturn(Page.empty(pageable));

            outreachService.findAll(otherTenantId, pageable);

            verify(outreachRepository).findByTenantId(otherTenantId, pageable);
            verify(outreachRepository, never()).findByTenantId(TENANT_ID, pageable);
        }

        @Test
        @DisplayName("should isolate outreach by tenant in findById")
        void shouldIsolateOutreachByTenantInFindById() {
            UUID otherTenantId = UUID.randomUUID();

            outreachService.findById(otherTenantId, OUTREACH_ID);

            verify(outreachRepository).findByIdAndTenantId(OUTREACH_ID, otherTenantId);
        }

        @Test
        @DisplayName("should set tenant ID when creating outreach")
        void shouldSetTenantIdWhenCreatingOutreach() {
            when(linkedInConfig.getOutreach()).thenReturn(outreachConfig);
            when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(testLead));
            when(outreachRepository.existsByProfileAndType(any(), any(), any())).thenReturn(false);
            when(outreachRepository.countDailyOutreach(any(), any(), any(), any())).thenReturn(0L);
            when(outreachRepository.save(any())).thenAnswer(i -> {
                LinkedInOutreach saved = i.getArgument(0);
                saved.setId(OUTREACH_ID);
                return saved;
            });

            outreachService.scheduleConnectionRequest(TENANT_ID, LEAD_ID, "Hi!", "Campaign", null, USER_ID);

            ArgumentCaptor<LinkedInOutreach> captor = ArgumentCaptor.forClass(LinkedInOutreach.class);
            verify(outreachRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        }
    }
}
