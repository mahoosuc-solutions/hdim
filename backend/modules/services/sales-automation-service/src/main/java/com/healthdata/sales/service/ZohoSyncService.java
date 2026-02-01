package com.healthdata.sales.service;

import com.healthdata.sales.client.ZohoClient;
import com.healthdata.sales.client.ZohoOAuthService;
import com.healthdata.sales.config.ZohoConfig;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for bidirectional sync between HDIM and Zoho CRM
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZohoSyncService {

    private final ZohoClient zohoClient;
    private final ZohoOAuthService oAuthService;
    private final ZohoConfig zohoConfig;

    private final LeadRepository leadRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    private static final DateTimeFormatter ZOHO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    // Track last sync time for incremental sync
    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>(LocalDateTime.now().minusHours(1));

    // ==================== Outbound Sync (HDIM -> Zoho) ====================

    /**
     * Sync all unsynced leads to Zoho
     */
    @Transactional
    public SyncResult syncUnsyncedLeads(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        List<Lead> unsyncedLeads = leadRepository.findUnsyncedLeads(tenantId);
        int synced = 0;
        int failed = 0;

        for (Lead lead : unsyncedLeads) {
            ZohoClient.ZohoSyncResult result = zohoClient.syncLead(lead);
            if (result.isSuccess()) {
                lead.setZohoLeadId(result.getZohoId());
                leadRepository.save(lead);
                synced++;
            } else if (!result.isSkipped()) {
                failed++;
            }
        }

        log.info("Synced {}/{} leads to Zoho ({} failed)", synced, unsyncedLeads.size(), failed);
        return new SyncResult(synced, failed, unsyncedLeads.size() - synced - failed);
    }

    /**
     * Sync all unsynced accounts to Zoho
     */
    @Transactional
    public SyncResult syncUnsyncedAccounts(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        List<Account> unsyncedAccounts = accountRepository.findUnsyncedAccounts(tenantId);
        int synced = 0;
        int failed = 0;

        for (Account account : unsyncedAccounts) {
            ZohoClient.ZohoSyncResult result = zohoClient.syncAccount(account);
            if (result.isSuccess()) {
                account.setZohoAccountId(result.getZohoId());
                accountRepository.save(account);
                synced++;
            } else if (!result.isSkipped()) {
                failed++;
            }
        }

        log.info("Synced {}/{} accounts to Zoho ({} failed)", synced, unsyncedAccounts.size(), failed);
        return new SyncResult(synced, failed, unsyncedAccounts.size() - synced - failed);
    }

    /**
     * Sync all unsynced contacts to Zoho
     */
    @Transactional
    public SyncResult syncUnsyncedContacts(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        List<Contact> unsyncedContacts = contactRepository.findUnsyncedContacts(tenantId);
        int synced = 0;
        int failed = 0;

        for (Contact contact : unsyncedContacts) {
            ZohoClient.ZohoSyncResult result = zohoClient.syncContact(contact);
            if (result.isSuccess()) {
                contact.setZohoContactId(result.getZohoId());
                contactRepository.save(contact);
                synced++;
            } else if (!result.isSkipped()) {
                failed++;
            }
        }

        log.info("Synced {}/{} contacts to Zoho ({} failed)", synced, unsyncedContacts.size(), failed);
        return new SyncResult(synced, failed, unsyncedContacts.size() - synced - failed);
    }

    /**
     * Sync all unsynced opportunities to Zoho
     */
    @Transactional
    public SyncResult syncUnsyncedOpportunities(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        List<Opportunity> unsyncedOpportunities = opportunityRepository.findUnsyncedOpportunities(tenantId);
        int synced = 0;
        int failed = 0;

        for (Opportunity opportunity : unsyncedOpportunities) {
            ZohoClient.ZohoSyncResult result = zohoClient.syncOpportunity(opportunity);
            if (result.isSuccess()) {
                opportunity.setZohoOpportunityId(result.getZohoId());
                opportunityRepository.save(opportunity);
                synced++;
            } else if (!result.isSkipped()) {
                failed++;
            }
        }

        log.info("Synced {}/{} opportunities to Zoho ({} failed)", synced, unsyncedOpportunities.size(), failed);
        return new SyncResult(synced, failed, unsyncedOpportunities.size() - synced - failed);
    }

    // ==================== Inbound Sync (Zoho -> HDIM) ====================

    /**
     * Pull modified leads from Zoho
     */
    @Transactional
    public SyncResult pullLeadsFromZoho(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        String modifiedSince = formatDateTime(lastSyncTime.get());
        List<Map<String, Object>> zohoLeads = zohoClient.fetchModifiedRecords("Leads", modifiedSince);

        int updated = 0;
        int created = 0;
        int skipped = 0;

        for (Map<String, Object> zohoLead : zohoLeads) {
            String zohoId = (String) zohoLead.get("id");
            String hdimId = (String) zohoLead.get("HDIM_Lead_ID");

            if (hdimId != null && !hdimId.isBlank()) {
                // Update existing HDIM record
                Optional<Lead> existingLead = leadRepository.findById(UUID.fromString(hdimId));
                if (existingLead.isPresent()) {
                    updateLeadFromZoho(existingLead.get(), zohoLead);
                    leadRepository.save(existingLead.get());
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // Check by email
                String email = (String) zohoLead.get("Email");
                if (email != null) {
                    Optional<Lead> existingLead = leadRepository.findByEmailAndTenantId(email, tenantId);
                    if (existingLead.isPresent()) {
                        existingLead.get().setZohoLeadId(zohoId);
                        updateLeadFromZoho(existingLead.get(), zohoLead);
                        leadRepository.save(existingLead.get());
                        updated++;
                    } else {
                        // Create new lead
                        Lead newLead = createLeadFromZoho(tenantId, zohoLead);
                        leadRepository.save(newLead);
                        created++;
                    }
                } else {
                    skipped++;
                }
            }
        }

        log.info("Pulled leads from Zoho: {} updated, {} created, {} skipped", updated, created, skipped);
        return new SyncResult(updated + created, 0, skipped);
    }

    /**
     * Pull modified accounts from Zoho
     */
    @Transactional
    public SyncResult pullAccountsFromZoho(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        String modifiedSince = formatDateTime(lastSyncTime.get());
        List<Map<String, Object>> zohoAccounts = zohoClient.fetchModifiedRecords("Accounts", modifiedSince);

        int updated = 0;
        int created = 0;
        int skipped = 0;

        for (Map<String, Object> zohoAccount : zohoAccounts) {
            String zohoId = (String) zohoAccount.get("id");
            String hdimId = (String) zohoAccount.get("HDIM_Account_ID");

            if (hdimId != null && !hdimId.isBlank()) {
                Optional<Account> existingAccount = accountRepository.findById(UUID.fromString(hdimId));
                if (existingAccount.isPresent()) {
                    updateAccountFromZoho(existingAccount.get(), zohoAccount);
                    accountRepository.save(existingAccount.get());
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // Check by Zoho ID
                Optional<Account> existingAccount = accountRepository.findByZohoAccountId(zohoId);
                if (existingAccount.isPresent()) {
                    updateAccountFromZoho(existingAccount.get(), zohoAccount);
                    accountRepository.save(existingAccount.get());
                    updated++;
                } else {
                    // Create new account
                    Account newAccount = createAccountFromZoho(tenantId, zohoAccount);
                    accountRepository.save(newAccount);
                    created++;
                }
            }
        }

        log.info("Pulled accounts from Zoho: {} updated, {} created, {} skipped", updated, created, skipped);
        return new SyncResult(updated + created, 0, skipped);
    }

    /**
     * Pull modified contacts from Zoho
     */
    @Transactional
    public SyncResult pullContactsFromZoho(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        String modifiedSince = formatDateTime(lastSyncTime.get());
        List<Map<String, Object>> zohoContacts = zohoClient.fetchModifiedRecords("Contacts", modifiedSince);

        int updated = 0;
        int created = 0;
        int skipped = 0;

        for (Map<String, Object> zohoContact : zohoContacts) {
            String zohoId = (String) zohoContact.get("id");
            String hdimId = (String) zohoContact.get("HDIM_Contact_ID");

            if (hdimId != null && !hdimId.isBlank()) {
                Optional<Contact> existingContact = contactRepository.findById(UUID.fromString(hdimId));
                if (existingContact.isPresent()) {
                    updateContactFromZoho(existingContact.get(), zohoContact);
                    contactRepository.save(existingContact.get());
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                String email = (String) zohoContact.get("Email");
                if (email != null) {
                    Optional<Contact> existingContact = contactRepository.findByEmailAndTenantId(email, tenantId);
                    if (existingContact.isPresent()) {
                        existingContact.get().setZohoContactId(zohoId);
                        updateContactFromZoho(existingContact.get(), zohoContact);
                        contactRepository.save(existingContact.get());
                        updated++;
                    } else {
                        Contact newContact = createContactFromZoho(tenantId, zohoContact);
                        contactRepository.save(newContact);
                        created++;
                    }
                } else {
                    skipped++;
                }
            }
        }

        log.info("Pulled contacts from Zoho: {} updated, {} created, {} skipped", updated, created, skipped);
        return new SyncResult(updated + created, 0, skipped);
    }

    /**
     * Pull modified deals from Zoho
     */
    @Transactional
    public SyncResult pullDealsFromZoho(UUID tenantId) {
        if (!isSyncEnabled()) {
            return SyncResult.disabled();
        }

        String modifiedSince = formatDateTime(lastSyncTime.get());
        List<Map<String, Object>> zohoDeals = zohoClient.fetchModifiedRecords("Deals", modifiedSince);

        int updated = 0;
        int skipped = 0;

        for (Map<String, Object> zohoDeal : zohoDeals) {
            String zohoId = (String) zohoDeal.get("id");
            String hdimId = (String) zohoDeal.get("HDIM_Opportunity_ID");

            if (hdimId != null && !hdimId.isBlank()) {
                Optional<Opportunity> existingOpp = opportunityRepository.findById(UUID.fromString(hdimId));
                if (existingOpp.isPresent()) {
                    updateOpportunityFromZoho(existingOpp.get(), zohoDeal);
                    opportunityRepository.save(existingOpp.get());
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                Optional<Opportunity> existingOpp = opportunityRepository.findByZohoOpportunityId(zohoId);
                if (existingOpp.isPresent()) {
                    updateOpportunityFromZoho(existingOpp.get(), zohoDeal);
                    opportunityRepository.save(existingOpp.get());
                    updated++;
                } else {
                    // Don't auto-create opportunities from Zoho - they require account association
                    skipped++;
                }
            }
        }

        log.info("Pulled deals from Zoho: {} updated, {} skipped", updated, skipped);
        return new SyncResult(updated, 0, skipped);
    }

    // ==================== Scheduled Sync ====================

    /**
     * Full bidirectional sync - runs every 15 minutes if enabled
     */
    @Scheduled(fixedRateString = "${zoho.sync.interval-minutes:15}000")
    @Transactional
    public void scheduledFullSync() {
        if (!isSyncEnabled()) {
            return;
        }

        log.info("Starting scheduled Zoho sync");
        LocalDateTime syncStart = LocalDateTime.now();

        try {
            // Get all unique tenant IDs from leads (covers most active tenants)
            List<UUID> tenantIds = leadRepository.findDistinctTenantIds();
            
            // If no tenants found and default tenant is configured, use it
            if (tenantIds.isEmpty() && zohoConfig.getSync().getDefaultTenantId() != null) {
                try {
                    UUID defaultTenantId = UUID.fromString(zohoConfig.getSync().getDefaultTenantId());
                    tenantIds = List.of(defaultTenantId);
                    log.debug("Using configured default tenant ID: {}", defaultTenantId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid default tenant ID in configuration: {}", 
                        zohoConfig.getSync().getDefaultTenantId());
                }
            }

            if (tenantIds.isEmpty()) {
                log.debug("No tenants found for Zoho sync - skipping");
                return;
            }

            log.info("Syncing {} tenant(s) with Zoho", tenantIds.size());

            // Sync each tenant
            for (UUID tenantId : tenantIds) {
                try {
                    log.debug("Syncing tenant: {}", tenantId);
                    
                    // Push to Zoho
                    syncUnsyncedLeads(tenantId);
                    syncUnsyncedAccounts(tenantId);
                    syncUnsyncedContacts(tenantId);
                    syncUnsyncedOpportunities(tenantId);

                    // Pull from Zoho
                    pullLeadsFromZoho(tenantId);
                    pullAccountsFromZoho(tenantId);
                    pullContactsFromZoho(tenantId);
                    pullDealsFromZoho(tenantId);
                } catch (Exception e) {
                    log.error("Error syncing tenant {}: {}", tenantId, e.getMessage(), e);
                    // Continue with next tenant
                }
            }

            lastSyncTime.set(syncStart);
            log.info("Scheduled Zoho sync completed for {} tenant(s)", tenantIds.size());
        } catch (Exception e) {
            log.error("Scheduled Zoho sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual full sync for a specific tenant
     */
    @Transactional
    public FullSyncResult fullSync(UUID tenantId) {
        if (!isSyncEnabled()) {
            return FullSyncResult.disabled();
        }

        log.info("Starting full Zoho sync for tenant {}", tenantId);
        LocalDateTime syncStart = LocalDateTime.now();

        FullSyncResult result = new FullSyncResult();

        try {
            // Push to Zoho
            result.setLeadsPushed(syncUnsyncedLeads(tenantId));
            result.setAccountsPushed(syncUnsyncedAccounts(tenantId));
            result.setContactsPushed(syncUnsyncedContacts(tenantId));
            result.setOpportunitiesPushed(syncUnsyncedOpportunities(tenantId));

            // Pull from Zoho
            result.setLeadsPulled(pullLeadsFromZoho(tenantId));
            result.setAccountsPulled(pullAccountsFromZoho(tenantId));
            result.setContactsPulled(pullContactsFromZoho(tenantId));
            result.setOpportunitiesPulled(pullDealsFromZoho(tenantId));

            lastSyncTime.set(syncStart);
            result.setSuccess(true);
            log.info("Full Zoho sync completed for tenant {}", tenantId);
        } catch (Exception e) {
            log.error("Full Zoho sync failed for tenant {}: {}", tenantId, e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    // ==================== Mapping Helpers ====================

    private void updateLeadFromZoho(Lead lead, Map<String, Object> zohoData) {
        if (zohoData.get("First_Name") != null) lead.setFirstName((String) zohoData.get("First_Name"));
        if (zohoData.get("Last_Name") != null) lead.setLastName((String) zohoData.get("Last_Name"));
        if (zohoData.get("Phone") != null) lead.setPhone((String) zohoData.get("Phone"));
        if (zohoData.get("Company") != null) lead.setCompany((String) zohoData.get("Company"));
        if (zohoData.get("Designation") != null) lead.setTitle((String) zohoData.get("Designation"));
        if (zohoData.get("State") != null) lead.setState((String) zohoData.get("State"));
        // Map status back
        String zohoStatus = (String) zohoData.get("Lead_Status");
        if (zohoStatus != null) {
            lead.setStatus(mapZohoLeadStatus(zohoStatus));
        }
    }

    private Lead createLeadFromZoho(UUID tenantId, Map<String, Object> zohoData) {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setTenantId(tenantId);
        lead.setZohoLeadId((String) zohoData.get("id"));
        lead.setFirstName((String) zohoData.get("First_Name"));
        lead.setLastName((String) zohoData.get("Last_Name"));
        lead.setEmail((String) zohoData.get("Email"));
        lead.setPhone((String) zohoData.get("Phone"));
        lead.setCompany((String) zohoData.get("Company"));
        lead.setTitle((String) zohoData.get("Designation"));
        lead.setState((String) zohoData.get("State"));
        lead.setSource(LeadSource.OTHER);
        lead.setStatus(mapZohoLeadStatus((String) zohoData.get("Lead_Status")));
        return lead;
    }

    private void updateAccountFromZoho(Account account, Map<String, Object> zohoData) {
        if (zohoData.get("Account_Name") != null) account.setName((String) zohoData.get("Account_Name"));
        if (zohoData.get("Website") != null) account.setWebsite((String) zohoData.get("Website"));
        if (zohoData.get("Phone") != null) account.setPhone((String) zohoData.get("Phone"));
        if (zohoData.get("Billing_State") != null) account.setState((String) zohoData.get("Billing_State"));
        if (zohoData.get("Industry") != null) account.setIndustry((String) zohoData.get("Industry"));
    }

    private Account createAccountFromZoho(UUID tenantId, Map<String, Object> zohoData) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setTenantId(tenantId);
        account.setZohoAccountId((String) zohoData.get("id"));
        account.setName((String) zohoData.get("Account_Name"));
        account.setWebsite((String) zohoData.get("Website"));
        account.setPhone((String) zohoData.get("Phone"));
        account.setState((String) zohoData.get("Billing_State"));
        account.setIndustry((String) zohoData.get("Industry"));
        account.setStage(AccountStage.PROSPECT);
        account.setOrganizationType(OrganizationType.OTHER);
        return account;
    }

    private void updateContactFromZoho(Contact contact, Map<String, Object> zohoData) {
        if (zohoData.get("First_Name") != null) contact.setFirstName((String) zohoData.get("First_Name"));
        if (zohoData.get("Last_Name") != null) contact.setLastName((String) zohoData.get("Last_Name"));
        if (zohoData.get("Phone") != null) contact.setPhone((String) zohoData.get("Phone"));
        if (zohoData.get("Title") != null) contact.setTitle((String) zohoData.get("Title"));
        if (zohoData.get("Department") != null) contact.setDepartment((String) zohoData.get("Department"));
    }

    private Contact createContactFromZoho(UUID tenantId, Map<String, Object> zohoData) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setTenantId(tenantId);
        contact.setZohoContactId((String) zohoData.get("id"));
        contact.setFirstName((String) zohoData.get("First_Name"));
        contact.setLastName((String) zohoData.get("Last_Name"));
        contact.setEmail((String) zohoData.get("Email"));
        contact.setPhone((String) zohoData.get("Phone"));
        contact.setTitle((String) zohoData.get("Title"));
        contact.setDepartment((String) zohoData.get("Department"));
        contact.setContactType(ContactType.USER);
        return contact;
    }

    private void updateOpportunityFromZoho(Opportunity opportunity, Map<String, Object> zohoData) {
        if (zohoData.get("Deal_Name") != null) opportunity.setName((String) zohoData.get("Deal_Name"));
        if (zohoData.get("Amount") != null) {
            Object amount = zohoData.get("Amount");
            if (amount instanceof Number) {
                opportunity.setAmount(BigDecimal.valueOf(((Number) amount).doubleValue()));
            }
        }
        if (zohoData.get("Probability") != null) {
            Object prob = zohoData.get("Probability");
            if (prob instanceof Number) {
                opportunity.setProbability(((Number) prob).intValue());
            }
        }
        if (zohoData.get("Closing_Date") != null) {
            String dateStr = (String) zohoData.get("Closing_Date");
            opportunity.setExpectedCloseDate(LocalDate.parse(dateStr));
        }
        String zohoStage = (String) zohoData.get("Stage");
        if (zohoStage != null) {
            opportunity.setStage(mapZohoOpportunityStage(zohoStage));
        }
    }

    private LeadStatus mapZohoLeadStatus(String zohoStatus) {
        if (zohoStatus == null) return LeadStatus.NEW;
        return switch (zohoStatus) {
            case "Not Contacted" -> LeadStatus.NEW;
            case "Attempted to Contact" -> LeadStatus.CONTACTED;
            case "Contact in Future" -> LeadStatus.ENGAGED;
            case "Junk Lead" -> LeadStatus.UNQUALIFIED;
            case "Converted" -> LeadStatus.CONVERTED;
            case "Lost Lead" -> LeadStatus.LOST;
            default -> LeadStatus.NEW;
        };
    }

    private OpportunityStage mapZohoOpportunityStage(String zohoStage) {
        if (zohoStage == null) return OpportunityStage.DISCOVERY;
        return switch (zohoStage) {
            case "Qualification" -> OpportunityStage.DISCOVERY;
            case "Needs Analysis" -> OpportunityStage.DEMO;
            case "Value Proposition" -> OpportunityStage.PROPOSAL;
            case "Negotiation/Review" -> OpportunityStage.NEGOTIATION;
            case "Proposal/Price Quote" -> OpportunityStage.CONTRACT;
            case "Closed Won" -> OpportunityStage.CLOSED_WON;
            case "Closed Lost" -> OpportunityStage.CLOSED_LOST;
            default -> OpportunityStage.DISCOVERY;
        };
    }

    private boolean isSyncEnabled() {
        return zohoConfig.getSync().isEnabled() && oAuthService.isConfigured();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.atOffset(ZoneOffset.UTC).format(ZOHO_DATE_FORMAT);
    }

    // ==================== Result Classes ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SyncResult {
        private int synced;
        private int failed;
        private int skipped;

        public static SyncResult disabled() {
            return new SyncResult(0, 0, 0);
        }
    }

    @lombok.Data
    public static class FullSyncResult {
        private boolean success;
        private String errorMessage;
        private SyncResult leadsPushed;
        private SyncResult accountsPushed;
        private SyncResult contactsPushed;
        private SyncResult opportunitiesPushed;
        private SyncResult leadsPulled;
        private SyncResult accountsPulled;
        private SyncResult contactsPulled;
        private SyncResult opportunitiesPulled;

        public static FullSyncResult disabled() {
            FullSyncResult result = new FullSyncResult();
            result.setSuccess(false);
            result.setErrorMessage("Zoho sync is disabled");
            return result;
        }
    }
}
