package com.healthdata.costanalysis.application;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class CostTrackingAspect {

    private final CostTrackingCollectorService collectorService;

    @Around("@annotation(trackCost)")
    public Object trackExecutionCost(ProceedingJoinPoint joinPoint, TrackCost trackCost) throws Throwable {
        long startNs = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            String tenantId = tenantIdFromRequest();
            collectorService.recordMethodExecution(
                tenantId,
                trackCost.serviceId(),
                trackCost.metricType(),
                trackCost.featureKey().isBlank() ? null : trackCost.featureKey(),
                durationMs
            );
        }
    }

    private String tenantIdFromRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String tenantId = request.getHeader("X-Tenant-ID");
            if (tenantId != null && !tenantId.isBlank()) {
                return tenantId;
            }
        }
        return "UNKNOWN_TENANT";
    }
}
