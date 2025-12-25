package com.healthdata.sales.service;

import com.healthdata.sales.client.ZohoClient;
import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadConversionRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.exception.InvalidStageTransitionException;
import com.healthdata.sales.exception.LeadNotFoundException;
import com.healthdata.sales.mapper.LeadMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private LeadMapper leadMapper;

    @Mock
    private ZohoClient zohoClient;

    @Mock
    private EmailSequenceService emailSequenceService;

    @Mock
    private EmailSequenceRepository emailSequenceRepository;

    @InjectMocks
    private LeadService leadService;

    private UUID tenantId;
    private UUID leadId;
    private Lead testLead;
    private LeadDTO testLeadDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();

        testLead = createTestLead();
        testLeadDTO = createTestLeadDTO();
    }

    private Lead createTestLead() {
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId(tenantId);
        lead.setFirstName("John");
        lead.setLastName("Doe");
        lead.setEmail("john.doe@example.com");
        lead.setCompany("Test Company");
        lead.setPhone("555-1234");
        lead.setSource(LeadSource.WEBSITE);
        lead.setStatus(LeadStatus.NEW);
        lead.setScore(75);
        return lead;
    }

    private LeadDTO createTestLeadDTO() {
        return LeadDTO.builder()
            .id(leadId)
            .tenantId(tenantId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .company("Test Company")
            .phone("555-1234")
            .source(LeadSource.WEBSITE)
            .status(LeadStatus.NEW)
            .score(75)
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return lead when found")
        void shouldReturnLeadWhenFound() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            LeadDTO result = leadService.findById(tenantId, leadId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(leadId);
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            verify(leadRepository).findByIdAndTenantId(leadId, tenantId);
        }

        @Test
        @DisplayName("should throw LeadNotFoundException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.findById(tenantId, leadId))
                .isInstanceOf(LeadNotFoundException.class)
                .hasMessageContaining(leadId.toString());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated leads")
        void shouldReturnPaginatedLeads() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Lead> leadPage = new PageImpl<>(List.of(testLead), pageable, 1);

            when(leadRepository.findByTenantId(tenantId, pageable)).thenReturn(leadPage);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            Page<LeadDTO> result = leadService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new lead")
        void shouldCreateAndReturnNewLead() {
            when(leadMapper.toEntity(any(LeadDTO.class))).thenReturn(testLead);
            when(leadRepository.save(any(Lead.class))).thenReturn(testLead);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            LeadDTO result = leadService.create(tenantId, testLeadDTO);

            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            verify(leadRepository).save(any(Lead.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing lead")
        void shouldUpdateExistingLead() {
            LeadDTO updateDTO = LeadDTO.builder()
                .firstName("Jane")
                .lastName("Updated")
                .email("jane.updated@example.com")
                .build();

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(leadRepository.save(any(Lead.class))).thenReturn(testLead);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            LeadDTO result = leadService.update(tenantId, leadId, updateDTO);

            assertThat(result).isNotNull();
            verify(leadMapper).updateEntity(testLead, updateDTO);
            verify(leadRepository).save(testLead);
        }

        @Test
        @DisplayName("should throw LeadNotFoundException when updating non-existent lead")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.update(tenantId, leadId, testLeadDTO))
                .isInstanceOf(LeadNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing lead")
        void shouldDeleteExistingLead() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));

            leadService.delete(tenantId, leadId);

            verify(leadRepository).delete(testLead);
        }

        @Test
        @DisplayName("should throw LeadNotFoundException when deleting non-existent lead")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> leadService.delete(tenantId, leadId))
                .isInstanceOf(LeadNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("captureLead")
    class CaptureLead {

        @Test
        @DisplayName("should capture new lead successfully")
        void shouldCaptureNewLeadSuccessfully() {
            LeadCaptureRequest request = new LeadCaptureRequest();
            request.setFirstName("New");
            request.setLastName("Lead");
            request.setEmail("new.lead@example.com");
            request.setSource(LeadSource.WEBSITE);

            when(leadRepository.existsByEmailAndTenantId("new.lead@example.com", tenantId))
                .thenReturn(false);
            when(leadMapper.fromCaptureRequest(request, tenantId)).thenReturn(testLead);
            when(leadRepository.save(any(Lead.class))).thenReturn(testLead);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);
            when(emailSequenceRepository.findActiveSequencesForTargetType(eq(tenantId), eq(TargetType.LEAD)))
                .thenReturn(Collections.emptyList());

            LeadDTO result = leadService.captureLead(tenantId, request);

            assertThat(result).isNotNull();
            verify(leadRepository).save(any(Lead.class));
        }

        @Test
        @DisplayName("should update existing lead when email already exists")
        void shouldUpdateExistingLeadWhenEmailExists() {
            LeadCaptureRequest request = new LeadCaptureRequest();
            request.setEmail("john.doe@example.com");
            request.setNotes("Updated notes");

            when(leadRepository.existsByEmailAndTenantId("john.doe@example.com", tenantId))
                .thenReturn(true);
            when(leadRepository.findByEmailAndTenantId("john.doe@example.com", tenantId))
                .thenReturn(Optional.of(testLead));
            when(leadRepository.save(any(Lead.class))).thenReturn(testLead);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            LeadDTO result = leadService.captureLead(tenantId, request);

            assertThat(result).isNotNull();
            verify(leadRepository).save(testLead);
        }
    }

    @Nested
    @DisplayName("convertLead")
    class ConvertLead {

        @Test
        @DisplayName("should convert lead to account, contact, and opportunity")
        void shouldConvertLeadSuccessfully() {
            LeadConversionRequest request = new LeadConversionRequest();
            request.setAccountName("New Account");
            request.setOpportunityName("New Opportunity");
            request.setOpportunityAmount(new BigDecimal("50000"));
            request.setExpectedCloseDate(LocalDate.now().plusMonths(3));

            Account newAccount = new Account();
            newAccount.setId(UUID.randomUUID());
            newAccount.setTenantId(tenantId);

            Contact newContact = new Contact();
            newContact.setId(UUID.randomUUID());
            newContact.setTenantId(tenantId);

            Opportunity newOpportunity = new Opportunity();
            newOpportunity.setId(UUID.randomUUID());
            newOpportunity.setTenantId(tenantId);

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));
            when(accountRepository.save(any(Account.class))).thenReturn(newAccount);
            when(contactRepository.save(any(Contact.class))).thenReturn(newContact);
            when(opportunityRepository.save(any(Opportunity.class))).thenReturn(newOpportunity);
            when(leadRepository.save(any(Lead.class))).thenReturn(testLead);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            LeadDTO result = leadService.convertLead(tenantId, leadId, request);

            assertThat(result).isNotNull();
            verify(accountRepository).save(any(Account.class));
            verify(contactRepository).save(any(Contact.class));
            verify(opportunityRepository).save(any(Opportunity.class));
        }

        @Test
        @DisplayName("should throw exception when lead is already converted")
        void shouldThrowExceptionWhenAlreadyConverted() {
            testLead.setStatus(LeadStatus.CONVERTED);
            LeadConversionRequest request = new LeadConversionRequest();

            when(leadRepository.findByIdAndTenantId(leadId, tenantId))
                .thenReturn(Optional.of(testLead));

            assertThatThrownBy(() -> leadService.convertLead(tenantId, leadId, request))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("already converted");
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatus {

        @Test
        @DisplayName("should return leads filtered by status")
        void shouldReturnLeadsFilteredByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Lead> leadPage = new PageImpl<>(List.of(testLead), pageable, 1);

            when(leadRepository.findByTenantIdAndStatus(tenantId, LeadStatus.NEW, pageable))
                .thenReturn(leadPage);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            Page<LeadDTO> result = leadService.findByStatus(tenantId, LeadStatus.NEW, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findHighScoreLeads")
    class FindHighScoreLeads {

        @Test
        @DisplayName("should return leads with score above threshold")
        void shouldReturnHighScoreLeads() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Lead> leadPage = new PageImpl<>(List.of(testLead), pageable, 1);

            when(leadRepository.findByTenantIdAndMinScore(tenantId, 70, pageable))
                .thenReturn(leadPage);
            when(leadMapper.toDTO(testLead)).thenReturn(testLeadDTO);

            Page<LeadDTO> result = leadService.findHighScoreLeads(tenantId, 70, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getScore()).isGreaterThanOrEqualTo(70);
        }
    }
}
