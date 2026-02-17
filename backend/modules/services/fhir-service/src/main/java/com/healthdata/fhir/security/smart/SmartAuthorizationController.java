package com.healthdata.fhir.security.smart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for SMART on FHIR OAuth 2.0 endpoints.
 *
 * Implements:
 * - GET /oauth/authorize - Authorization endpoint
 * - POST /oauth/token - Token endpoint
 * - POST /oauth/revoke - Token revocation
 * - GET /oauth/userinfo - User info (OpenID Connect)
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "SMART", description = "SMART on FHIR OAuth 2.0 endpoints")
public class SmartAuthorizationController {

    private final SmartAuthorizationService authorizationService;
    private final SmartClientRepository clientRepository;
    private final SmartLaunchContextStore launchContextStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(
        summary = "OAuth 2.0 Authorization",
        description = "Initiates the authorization code flow. Redirects to the client with an authorization code.",
        operationId = "authorize"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to client with authorization code or error"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping("/authorize")
    public RedirectView authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "aud", required = false) String audience,
            @RequestParam(value = "launch", required = false) String launch,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false, defaultValue = "S256") String codeChallengeMethod) {

        log.info("Authorization request: client_id={}, scope={}", clientId, scope);

        try {
            // Validate client
            SmartClient client = clientRepository.findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new SmartAuthorizationException("invalid_client", "Unknown client"));

            // Validate redirect URI
            if (!client.isValidRedirectUri(redirectUri)) {
                throw new SmartAuthorizationException("invalid_request", "Invalid redirect URI");
            }

            // Validate response type
            if (!"code".equals(responseType)) {
                return errorRedirect(redirectUri, "unsupported_response_type",
                    "Only 'code' response type is supported", state);
            }

            // Parse and validate scopes
            Set<String> requestedScopes = parseScopes(scope);
            for (String s : requestedScopes) {
                if (!client.isAllowedScope(s)) {
                    return errorRedirect(redirectUri, "invalid_scope",
                        "Scope not allowed: " + s, state);
                }
            }

            // Build launch context
            SmartLaunchContext launchContext = buildLaunchContext(launch, requestedScopes, audience);

            // Generate authorization code
            String code = authorizationService.generateAuthorizationCode(
                clientId, redirectUri, requestedScopes, state,
                codeChallenge, codeChallengeMethod, launchContext);

            // Build redirect URL with code
            String redirectUrl = buildRedirectUrl(redirectUri, code, state);
            log.info("Authorization successful for client: {}", clientId);

            return new RedirectView(redirectUrl);

        } catch (SmartAuthorizationException e) {
            log.warn("Authorization failed: {}", e.getMessage());
            return errorRedirect(redirectUri, e.getErrorCode(), e.getErrorDescription(), state);
        } catch (Exception e) {
            log.error("Authorization error", e);
            return errorRedirect(redirectUri, "server_error", "Internal server error", state);
        }
    }

    @Operation(
        summary = "OAuth 2.0 Token Exchange",
        description = "Exchanges authorization code, refresh token, or client credentials for an access token.",
        operationId = "token"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token issued successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request or grant"),
        @ApiResponse(responseCode = "401", description = "Invalid client credentials")
    })
    @PostMapping(
        path = "/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Token request: grant_type={}, client_id={}", grantType, clientId);

        try {
            // Extract client credentials from header if not in body
            if (clientId == null && authHeader != null && authHeader.startsWith("Basic ")) {
                String[] credentials = extractBasicCredentials(authHeader);
                clientId = credentials[0];
                clientSecret = credentials[1];
            }

            TokenResponse tokenResponse;

            switch (grantType) {
                case "authorization_code":
                    if (code == null || redirectUri == null || clientId == null) {
                        return errorResponse("invalid_request", "Missing required parameters");
                    }
                    tokenResponse = authorizationService.exchangeAuthorizationCode(
                        code, clientId, redirectUri, codeVerifier);
                    break;

                case "refresh_token":
                    if (refreshToken == null || clientId == null) {
                        return errorResponse("invalid_request", "Missing refresh_token or client_id");
                    }
                    tokenResponse = authorizationService.refreshAccessToken(refreshToken, clientId);
                    break;

                case "client_credentials":
                    if (clientId == null || clientSecret == null) {
                        return errorResponse("invalid_request", "Missing client credentials");
                    }
                    Set<String> scopes = parseScopes(scope);
                    tokenResponse = authorizationService.clientCredentialsGrant(
                        clientId, clientSecret, scopes);
                    break;

                default:
                    return errorResponse("unsupported_grant_type",
                        "Grant type not supported: " + grantType);
            }

            log.info("Token issued for client: {}", clientId);
            return ResponseEntity.ok(tokenResponse);

        } catch (SmartAuthorizationException e) {
            log.warn("Token request failed: {}", e.getMessage());
            return errorResponse(e.getErrorCode(), e.getErrorDescription());
        } catch (Exception e) {
            log.error("Token error", e);
            return errorResponse("server_error", "Internal server error");
        }
    }

    @Operation(
        summary = "Revoke Token",
        description = "Revokes an access token or refresh token.",
        operationId = "revoke"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token revoked successfully")
    })
    @PostMapping(
        path = "/revoke",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        log.info("Token revocation request");
        authorizationService.revokeToken(token, tokenTypeHint);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "OpenID Connect User Info",
        description = "Returns user information based on the access token. Part of OpenID Connect protocol.",
        operationId = "userinfo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User info returned successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    @GetMapping(path = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> userinfo(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (!authHeader.startsWith("Bearer ")) {
                return errorResponse("invalid_token", "Bearer token required");
            }

            String token = authHeader.substring(7);
            var claims = authorizationService.validateAccessToken(token);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("sub", claims.getSubject());

            if (claims.containsKey("fhirUser")) {
                userInfo.put("fhirUser", claims.get("fhirUser"));
            }
            if (claims.containsKey("patient")) {
                userInfo.put("patient", claims.get("patient"));
            }

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.warn("User info request failed: {}", e.getMessage());
            return errorResponse("invalid_token", "Token validation failed");
        }
    }

    @Operation(
        summary = "Token Introspection",
        description = "Validates a token and returns its metadata. Used by resource servers to verify tokens.",
        operationId = "introspect"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token introspection result (active true/false)")
    })
    @PostMapping(
        path = "/introspect",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> introspect(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        try {
            var claims = authorizationService.validateAccessToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("active", true);
            response.put("sub", claims.getSubject());
            response.put("client_id", claims.get("client_id"));
            response.put("scope", claims.get("scope"));
            response.put("exp", claims.getExpiration().getTime() / 1000);
            response.put("iat", claims.getIssuedAt().getTime() / 1000);
            response.put("iss", claims.getIssuer());
            response.put("aud", claims.getAudience());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Invalid token - return inactive
            Map<String, Object> response = new HashMap<>();
            response.put("active", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Parse scope string to set.
     */
    private Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return new HashSet<>();
        }
        return Arrays.stream(scope.split("\\s+"))
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
    }

    /**
     * Build launch context from parameters.
     */
    private SmartLaunchContext buildLaunchContext(String launch, Set<String> scopes, String audience) {
        SmartLaunchContext.SmartLaunchContextBuilder builder = SmartLaunchContext.builder();
        builder.audience(audience);

        if (StringUtils.hasText(launch)) {
            // EHR launch - decode or resolve launch parameter
            builder.standalone(false);
            resolveAndApplyLaunchContext(launch, builder);
        } else {
            // Standalone launch
            builder.standalone(true);
        }

        SmartLaunchContext context = builder.build();

        // Fallback defaults for standalone/demo flows when context is not provided by launch.
        if ((scopes.contains("launch/patient") || scopes.contains("patient/*.read"))
                && !StringUtils.hasText(context.getPatient())) {
            context.setPatient("demo-patient-001");
        }
        if (scopes.contains("launch/encounter") && !StringUtils.hasText(context.getEncounter())) {
            context.setEncounter("demo-encounter-001");
        }
        if (scopes.contains("fhirUser") && !StringUtils.hasText(context.getFhirUser())) {
            context.setFhirUser("Practitioner/demo-practitioner-001");
        }

        return context;
    }

    /**
     * Apply launch context values if the launch parameter is decodable.
     * Supported formats:
     * - raw JSON object string
     * - base64url-encoded JSON object
     * - JWT/JWS where payload is base64url-encoded JSON object
     */
    private void resolveAndApplyLaunchContext(String launch, SmartLaunchContext.SmartLaunchContextBuilder builder) {
        builder.launch(launch);
        try {
            Map<String, Object> launchContext = parseLaunchParameter(launch);
            if (launchContext != null && !launchContext.isEmpty()) {
                // Replace inline launch payload with opaque launch ID for downstream continuity.
                String opaqueLaunchId = launchContextStore.storeLaunchContext(launchContext);
                builder.launch(opaqueLaunchId);
                applyLaunchContext(launchContext, builder);
                return;
            }

            Optional<Map<String, Object>> storedLaunchContext = Optional.ofNullable(
                launchContextStore.resolveLaunchContext(launch)).orElse(Optional.empty());

            if (storedLaunchContext.isPresent()) {
                applyLaunchContext(storedLaunchContext.get(), builder);
                return;
            }

            log.debug("Launch parameter not decodable and no opaque context found. Falling back to scope-based context. launch={}", launch);
        } catch (Exception e) {
            log.debug("Launch parameter resolution failed; falling back to scope-based context. launch={}", launch);
        }
    }

    private void applyLaunchContext(Map<String, Object> launchContext, SmartLaunchContext.SmartLaunchContextBuilder builder) {
        builder.patient(asString(launchContext.get("patient")));
        builder.encounter(asString(launchContext.get("encounter")));
        builder.fhirUser(asString(launchContext.get("fhirUser")));
        builder.tenant(asString(launchContext.get("tenant")));
        builder.intent(asString(launchContext.get("intent")));
        builder.smartStyleUrl(asString(launchContext.get("smart_style_url")));
        builder.needPatientBanner(asBoolean(launchContext.get("need_patient_banner")));
    }

    private Map<String, Object> parseLaunchParameter(String launch) {
        String raw = launch == null ? "" : launch.trim();
        if (raw.isEmpty()) {
            return Map.of();
        }

        // 1) Plain JSON object
        if (raw.startsWith("{") && raw.endsWith("}")) {
            return parseJsonObject(raw);
        }

        // 2) JWT/JWS token -> parse payload segment
        String[] jwtParts = raw.split("\\.");
        if (jwtParts.length == 3 && StringUtils.hasText(jwtParts[1])) {
            String payloadJson = decodeBase64Url(jwtParts[1]);
            if (payloadJson.startsWith("{") && payloadJson.endsWith("}")) {
                return parseJsonObject(payloadJson);
            }
        }

        // 3) Base64url JSON object
        String decoded = decodeBase64Url(raw);
        if (decoded.startsWith("{") && decoded.endsWith("}")) {
            return parseJsonObject(decoded);
        }

        return Map.of();
    }

    private Map<String, Object> parseJsonObject(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String decodeBase64Url(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value));
        } catch (Exception e) {
            return "";
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Build redirect URL with authorization code.
     */
    private String buildRedirectUrl(String redirectUri, String code, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("code=").append(code);
        if (StringUtils.hasText(state)) {
            url.append("&state=").append(state);
        }
        return url.toString();
    }

    /**
     * Build error redirect.
     */
    private RedirectView errorRedirect(String redirectUri, String error, String description, String state) {
        StringBuilder url = new StringBuilder(redirectUri);
        url.append(redirectUri.contains("?") ? "&" : "?");
        url.append("error=").append(error);
        url.append("&error_description=").append(description.replace(" ", "+"));
        if (StringUtils.hasText(state)) {
            url.append("&state=").append(state);
        }
        return new RedirectView(url.toString());
    }

    /**
     * Build error response.
     */
    private ResponseEntity<Map<String, String>> errorResponse(String error, String description) {
        Map<String, String> body = new HashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Extract credentials from Basic auth header.
     */
    private String[] extractBasicCredentials(String authHeader) {
        String base64Credentials = authHeader.substring(6);
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        return credentials.split(":", 2);
    }
}
