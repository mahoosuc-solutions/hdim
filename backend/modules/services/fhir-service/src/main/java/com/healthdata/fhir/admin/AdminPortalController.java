package com.healthdata.fhir.admin;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.auth.context.ScopedTenant;

import com.healthdata.fhir.admin.model.ApiPreset;
import com.healthdata.fhir.admin.model.DashboardSnapshot;
import com.healthdata.fhir.admin.model.ServiceCatalog;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot;

@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminPortalController {

    private final AdminPortalService adminPortalService;

    public AdminPortalController(AdminPortalService adminPortalService) {
        this.adminPortalService = adminPortalService;
    }

    @GetMapping("/dashboard")
    public DashboardSnapshot dashboard(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return adminPortalService.getDashboardSnapshot(resolveTenant(tenantId));
    }

    @GetMapping("/service-catalog")
    public ServiceCatalog serviceCatalog(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return adminPortalService.getServiceCatalog(resolveTenant(tenantId));
    }

    @GetMapping("/system-health")
    public SystemHealthSnapshot systemHealth(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return adminPortalService.getSystemHealth(resolveTenant(tenantId));
    }

    @GetMapping("/api-presets")
    public List<ApiPreset> apiPresets(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        return adminPortalService.getApiPresets(resolveTenant(tenantId));
    }

    private String resolveTenant(String tenantId) {
        return ScopedTenant.currentTenant().orElseGet(() ->
                (tenantId == null || tenantId.isBlank()) ? "tenant-1" : tenantId);
    }
}
