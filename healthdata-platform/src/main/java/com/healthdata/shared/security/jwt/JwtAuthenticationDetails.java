package com.healthdata.shared.security.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Details stored in Authentication to expose JWT context.
 */
@Getter
@RequiredArgsConstructor
public class JwtAuthenticationDetails {

    private final String token;
    private final String userId;
    private final String tenantId;
}
