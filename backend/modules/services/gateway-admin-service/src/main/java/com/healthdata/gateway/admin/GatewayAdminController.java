package com.healthdata.gateway.admin;

import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway routing for admin and tooling services.
 */
@RestController
@RequiredArgsConstructor
public class GatewayAdminController {

    private final GatewayForwarder forwarder;

    @Value("${backend.services.agent-builder.url}")
    private String agentBuilderUrl;

    @Value("${backend.services.agent-runtime.url}")
    private String agentRuntimeUrl;

    @Value("${backend.services.sales-automation.url}")
    private String salesAutomationUrl;

    @Value("${backend.services.audit.url}")
    private String auditUrl;

    @Value("${backend.services.audit-base.url}")
    private String auditBaseUrl;

    @RequestMapping(value = "/api/v1/agent-builder/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAgentBuilder(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, agentBuilderUrl, "/api/v1/agent-builder");
    }

    @RequestMapping(value = "/api/v1/tools/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeTools(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, agentRuntimeUrl, "/api/v1/tools");
    }

    @RequestMapping(value = "/api/v1/providers/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeProviders(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, agentRuntimeUrl, "/api/v1/providers");
    }

    @RequestMapping(value = "/api/v1/runtime/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeHealth(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, agentRuntimeUrl, "/api/v1/runtime");
    }

    @RequestMapping(value = "/api/sales/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToSalesAutomation(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, salesAutomationUrl, "/api/sales");
    }

    @RequestMapping(value = "/sales-automation/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToSalesAutomationDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, salesAutomationUrl, "/sales-automation");
    }

    @RequestMapping(value = "/api/v1/audit/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAudit(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, auditUrl, "/api/v1/audit");
    }

    @RequestMapping(value = "/audit/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAuditDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, auditBaseUrl, "/audit");
    }
}
