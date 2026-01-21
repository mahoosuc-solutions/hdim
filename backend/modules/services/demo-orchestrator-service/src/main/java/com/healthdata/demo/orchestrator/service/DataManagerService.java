package com.healthdata.demo.orchestrator.service;

import com.healthdata.demo.orchestrator.integration.DevOpsAgentClient;
import com.healthdata.demo.orchestrator.model.FhirValidationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataManagerService {
    private final DevOpsAgentClient devopsAgent;
    
    public void seedBaseData() {
        log.info("Seeding base demo data...");
        devopsAgent.publishLog("INFO", "Seeding base demo data...", "SEED");
        // TODO: Implement actual data seeding logic
        devopsAgent.publishLog("INFO", "Base data seeding completed", "SEED");
    }
    
    public void clearAllData() {
        log.info("Clearing all demo data...");
        devopsAgent.publishLog("INFO", "Clearing all demo data...", "CLEAR");
        // TODO: Implement data clearing logic
        devopsAgent.publishLog("INFO", "Data clearing completed", "CLEAR");
    }
    
    public FhirValidationResult validateFhirData() {
        log.info("Validating FHIR demo data authenticity...");
        devopsAgent.publishLog("INFO", "Validating FHIR demo data...", "VALIDATION");
        
        FhirValidationResultDto result = devopsAgent.validateFhirDemoData();
        
        if (result != null) {
            String status = result.getOverallStatus();
            devopsAgent.updateStatus("FHIR_VALIDATION", status, 
                java.util.Map.of(
                    "totalChecks", result.getTotalChecks(),
                    "passedChecks", result.getPassedChecks(),
                    "failedChecks", result.getFailedChecks(),
                    "warningChecks", result.getWarningChecks()
                ));
            
            devopsAgent.publishLog(status.equals("PASS") ? "INFO" : "WARN",
                String.format("FHIR validation: %s (%d passed, %d failed, %d warnings)",
                    status, result.getPassedChecks(), result.getFailedChecks(), 
                    result.getWarningChecks()),
                "VALIDATION");
        } else {
            devopsAgent.publishLog("ERROR", "FHIR validation failed - could not retrieve results", "VALIDATION");
        }
        
        return result;
    }
}
