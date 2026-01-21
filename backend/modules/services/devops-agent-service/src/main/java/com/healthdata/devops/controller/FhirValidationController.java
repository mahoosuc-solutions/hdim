package com.healthdata.devops.controller;

import com.healthdata.devops.model.FhirValidationResult;
import com.healthdata.devops.validation.FhirDataValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devops/fhir-validation")
@RequiredArgsConstructor
public class FhirValidationController {
    private final FhirDataValidationService validationService;
    
    @PostMapping("/validate")
    public ResponseEntity<FhirValidationResult> validateDemoData() {
        FhirValidationResult result = validationService.validateDemoData();
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/status")
    public ResponseEntity<FhirValidationResult> getLastValidation() {
        FhirValidationResult result = validationService.validateDemoData();
        return ResponseEntity.ok(result);
    }
}
