package com.healthdata.sales.controller;

import com.healthdata.sales.service.EmailAutomationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email tracking (opens, clicks, unsubscribes)
 */
@RestController
@RequestMapping("/api/sales/email")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Email Tracking",
    description = """
        APIs for tracking email engagement and managing unsubscribes.

        These endpoints support email marketing automation:
        - Open tracking via invisible pixel
        - Click tracking with redirect
        - Unsubscribe handling for compliance

        Note: These are public endpoints (no authentication required)
        to allow tracking from email clients.
        """
)
public class EmailTrackingController {

    private final EmailAutomationService automationService;

    // 1x1 transparent GIF for tracking pixel
    private static final byte[] TRACKING_PIXEL = new byte[]{
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
        0x01, 0x00, (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        0x00, 0x00, 0x00, 0x21, (byte) 0xf9, 0x04, 0x01, 0x00,
        0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44,
        0x01, 0x00, 0x3b
    };

    /**
     * Track email open via invisible pixel
     */
    @GetMapping("/track/{trackingId}/open")
    @Operation(
        summary = "Track email open",
        description = """
            Tracking pixel endpoint for email opens.

            Returns a 1x1 transparent GIF that is embedded in emails.
            When the email client loads the image, the open is recorded.

            Headers are set to prevent caching to ensure accurate tracking.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tracking pixel returned, open recorded")
    })
    public ResponseEntity<byte[]> trackOpen(
        @Parameter(description = "Unique tracking ID for the email", required = true)
        @PathVariable String trackingId) {
        log.debug("Email open tracked: {}", trackingId);

        try {
            automationService.recordOpen(trackingId);
        } catch (Exception e) {
            log.warn("Error recording email open: {}", e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return new ResponseEntity<>(TRACKING_PIXEL, headers, HttpStatus.OK);
    }

    /**
     * Track link click and redirect
     */
    @GetMapping("/track/{trackingId}/click")
    @Operation(
        summary = "Track link click",
        description = """
            Tracks link clicks and redirects to the destination URL.

            All links in tracked emails are rewritten to pass through this endpoint.
            The click is recorded and the user is immediately redirected to the
            original destination URL via HTTP 302 redirect.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Click recorded, redirecting to destination")
    })
    public ResponseEntity<Void> trackClick(
        @Parameter(description = "Unique tracking ID for the email", required = true)
        @PathVariable String trackingId,
        @Parameter(description = "Destination URL to redirect to", required = true, example = "https://example.com/landing")
        @RequestParam String url
    ) {
        log.debug("Email click tracked: {} -> {}", trackingId, url);

        try {
            automationService.recordClick(trackingId);
        } catch (Exception e) {
            log.warn("Error recording email click: {}", e.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Unsubscribe page
     */
    @GetMapping("/unsubscribe/{token}")
    @Operation(
        summary = "Unsubscribe",
        description = """
            Processes unsubscribe requests from email recipients.

            Returns an HTML page confirming the unsubscribe action.
            The recipient is removed from the email sequence and marked
            as unsubscribed to prevent future emails.

            Required for CAN-SPAM and GDPR compliance.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unsubscribe processed, confirmation page returned")
    })
    public ResponseEntity<String> unsubscribe(
        @Parameter(description = "Unique unsubscribe token for the recipient", required = true)
        @PathVariable String token) {
        boolean success = automationService.processUnsubscribe(token);

        String html;
        if (success) {
            html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Unsubscribed</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f5f5f5; }
                        .container { text-align: center; padding: 40px; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #2e7d32; }
                        p { color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Successfully Unsubscribed</h1>
                        <p>You have been unsubscribed from this email sequence.</p>
                        <p>You will no longer receive emails from this campaign.</p>
                    </div>
                </body>
                </html>
                """;
        } else {
            html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Unsubscribe Error</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f5f5f5; }
                        .container { text-align: center; padding: 40px; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #d32f2f; }
                        p { color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Unsubscribe Error</h1>
                        <p>We couldn't process your unsubscribe request.</p>
                        <p>The link may have expired or already been used.</p>
                    </div>
                </body>
                </html>
                """;
        }

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }
}
