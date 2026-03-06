package com.healthdata.events.intelligence.security;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.events.intelligence.controller.ForbiddenIntelligenceOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntelligenceActorResolver {

    public String resolveRequiredActor(HttpServletRequest request) {
        Object userIdAttribute = request.getAttribute(AuthHeaderConstants.ATTR_USER_ID);
        if (userIdAttribute instanceof UUID userId) {
            return userId.toString();
        }

        if (userIdAttribute instanceof String userId && !userId.isBlank()) {
            return userId;
        }

        Object usernameAttribute = request.getAttribute(AuthHeaderConstants.ATTR_USERNAME);
        if (usernameAttribute instanceof String username && !username.isBlank()) {
            return username;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        throw new ForbiddenIntelligenceOperationException("Authenticated actor is required for this operation");
    }
}
