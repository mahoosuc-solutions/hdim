package com.healthdata.sales.service;

import com.healthdata.sales.dto.AccountDTO;
import com.healthdata.sales.entity.Account;
import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
import com.healthdata.sales.repository.AccountRepository;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private UUID tenantId;
    private UUID accountId;
    private Account testAccount;
    private AccountDTO testAccountDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testAccount = createTestAccount();
        testAccountDTO = createTestAccountDTO();
    }

    private Account createTestAccount() {
        Account account = new Account();
        account.setId(accountId);
        account.setTenantId(tenantId);
        account.setName("Test Healthcare ACO");
        account.setOrganizationType(OrganizationType.ACO);
        account.setWebsite("https://testhealthcare.com");
        account.setPhone("555-1234");
        account.setAddressLine1("123 Main St");
        account.setCity("Boston");
        account.setState("MA");
        account.setZipCode("02101");
        account.setPatientCount(25000);
        account.setEhrCount(3);
        account.setEhrSystems("Epic, Cerner");
        account.setStage(AccountStage.PROSPECT);
        account.setAnnualRevenue(50000000L);
        account.setEmployeeCount(500);
        account.setIndustry("Healthcare");
        account.setDescription("Large healthcare ACO");
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    private AccountDTO createTestAccountDTO() {
        return AccountDTO.builder()
            .id(accountId)
            .tenantId(tenantId)
            .name("Test Healthcare ACO")
            .organizationType(OrganizationType.ACO)
            .website("https://testhealthcare.com")
            .phone("555-1234")
            .addressLine1("123 Main St")
            .city("Boston")
            .state("MA")
            .zipCode("02101")
            .patientCount(25000)
            .ehrCount(3)
            .ehrSystems("Epic, Cerner")
            .stage(AccountStage.PROSPECT)
            .annualRevenue(50000000L)
            .employeeCount(500)
            .industry("Healthcare")
            .description("Large healthcare ACO")
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return account when found")
        void shouldReturnAccountWhenFound() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.of(testAccount));

            AccountDTO result = accountService.findById(tenantId, accountId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(accountId);
            assertThat(result.getName()).isEqualTo("Test Healthcare ACO");
            assertThat(result.getOrganizationType()).isEqualTo(OrganizationType.ACO);
            verify(accountRepository).findByIdAndTenantId(accountId, tenantId);
        }

        @Test
        @DisplayName("should throw RuntimeException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.findById(tenantId, accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated accounts")
        void shouldReturnPaginatedAccounts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount), pageable, 1);

            when(accountRepository.findByTenantId(tenantId, pageable)).thenReturn(accountPage);

            Page<AccountDTO> result = accountService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Test Healthcare ACO");
        }

        @Test
        @DisplayName("should return empty page when no accounts exist")
        void shouldReturnEmptyPageWhenNoAccounts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(accountRepository.findByTenantId(tenantId, pageable)).thenReturn(emptyPage);

            Page<AccountDTO> result = accountService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new account")
        void shouldCreateAndReturnNewAccount() {
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            AccountDTO result = accountService.create(tenantId, testAccountDTO);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Healthcare ACO");
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should set default stage to PROSPECT when not provided")
        void shouldSetDefaultStageWhenNotProvided() {
            AccountDTO dtoWithoutStage = AccountDTO.builder()
                .name("New Account")
                .build();

            Account savedAccount = new Account();
            savedAccount.setId(UUID.randomUUID());
            savedAccount.setTenantId(tenantId);
            savedAccount.setName("New Account");
            savedAccount.setStage(AccountStage.PROSPECT);

            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

            AccountDTO result = accountService.create(tenantId, dtoWithoutStage);

            assertThat(result).isNotNull();
            assertThat(result.getStage()).isEqualTo(AccountStage.PROSPECT);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing account")
        void shouldUpdateExistingAccount() {
            AccountDTO updateDTO = AccountDTO.builder()
                .name("Updated ACO Name")
                .patientCount(30000)
                .stage(AccountStage.CUSTOMER)
                .build();

            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

            AccountDTO result = accountService.update(tenantId, accountId, updateDTO);

            assertThat(result).isNotNull();
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            String originalName = testAccount.getName();
            AccountDTO updateDTO = AccountDTO.builder()
                .patientCount(35000)
                .build();

            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            accountService.update(tenantId, accountId, updateDTO);

            assertThat(testAccount.getName()).isEqualTo(originalName);
            assertThat(testAccount.getPatientCount()).isEqualTo(35000);
        }

        @Test
        @DisplayName("should throw RuntimeException when updating non-existent account")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.update(tenantId, accountId, testAccountDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing account")
        void shouldDeleteExistingAccount() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.of(testAccount));

            accountService.delete(tenantId, accountId);

            verify(accountRepository).delete(testAccount);
        }

        @Test
        @DisplayName("should throw RuntimeException when deleting non-existent account")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.delete(tenantId, accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("findByStage")
    class FindByStage {

        @Test
        @DisplayName("should return accounts filtered by stage")
        void shouldReturnAccountsFilteredByStage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount), pageable, 1);

            when(accountRepository.findByTenantIdAndStage(tenantId, AccountStage.PROSPECT, pageable))
                .thenReturn(accountPage);

            Page<AccountDTO> result = accountService.findByStage(tenantId, AccountStage.PROSPECT, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStage()).isEqualTo(AccountStage.PROSPECT);
        }

        @Test
        @DisplayName("should return empty page when no accounts match stage")
        void shouldReturnEmptyPageWhenNoAccountsMatchStage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(accountRepository.findByTenantIdAndStage(tenantId, AccountStage.CHURNED, pageable))
                .thenReturn(emptyPage);

            Page<AccountDTO> result = accountService.findByStage(tenantId, AccountStage.CHURNED, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByOrganizationType")
    class FindByOrganizationType {

        @Test
        @DisplayName("should return accounts filtered by organization type")
        void shouldReturnAccountsFilteredByOrgType() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount), pageable, 1);

            when(accountRepository.findByTenantIdAndOrganizationType(tenantId, OrganizationType.ACO, pageable))
                .thenReturn(accountPage);

            Page<AccountDTO> result = accountService.findByOrganizationType(tenantId, OrganizationType.ACO, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrganizationType()).isEqualTo(OrganizationType.ACO);
        }
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should return accounts matching search query")
        void shouldReturnAccountsMatchingSearchQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> accountPage = new PageImpl<>(List.of(testAccount), pageable, 1);

            when(accountRepository.searchByName(tenantId, "Healthcare", pageable))
                .thenReturn(accountPage);

            Page<AccountDTO> result = accountService.search(tenantId, "Healthcare", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).contains("Healthcare");
        }

        @Test
        @DisplayName("should return empty page when no accounts match search")
        void shouldReturnEmptyPageWhenNoAccountsMatchSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Account> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(accountRepository.searchByName(tenantId, "NonExistent", pageable))
                .thenReturn(emptyPage);

            Page<AccountDTO> result = accountService.search(tenantId, "NonExistent", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO Mapping")
    class DTOMapping {

        @Test
        @DisplayName("should correctly map all fields from entity to DTO")
        void shouldCorrectlyMapAllFieldsFromEntityToDTO() {
            when(accountRepository.findByIdAndTenantId(accountId, tenantId))
                .thenReturn(Optional.of(testAccount));

            AccountDTO result = accountService.findById(tenantId, accountId);

            assertThat(result.getId()).isEqualTo(testAccount.getId());
            assertThat(result.getTenantId()).isEqualTo(testAccount.getTenantId());
            assertThat(result.getName()).isEqualTo(testAccount.getName());
            assertThat(result.getOrganizationType()).isEqualTo(testAccount.getOrganizationType());
            assertThat(result.getWebsite()).isEqualTo(testAccount.getWebsite());
            assertThat(result.getPhone()).isEqualTo(testAccount.getPhone());
            assertThat(result.getAddressLine1()).isEqualTo(testAccount.getAddressLine1());
            assertThat(result.getCity()).isEqualTo(testAccount.getCity());
            assertThat(result.getState()).isEqualTo(testAccount.getState());
            assertThat(result.getZipCode()).isEqualTo(testAccount.getZipCode());
            assertThat(result.getPatientCount()).isEqualTo(testAccount.getPatientCount());
            assertThat(result.getEhrCount()).isEqualTo(testAccount.getEhrCount());
            assertThat(result.getEhrSystems()).isEqualTo(testAccount.getEhrSystems());
            assertThat(result.getStage()).isEqualTo(testAccount.getStage());
            assertThat(result.getAnnualRevenue()).isEqualTo(testAccount.getAnnualRevenue());
            assertThat(result.getEmployeeCount()).isEqualTo(testAccount.getEmployeeCount());
            assertThat(result.getIndustry()).isEqualTo(testAccount.getIndustry());
            assertThat(result.getDescription()).isEqualTo(testAccount.getDescription());
        }
    }
}
