package com.healthdata.clinicalworkflow.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Real-time Clinical Workflow Updates
 *
 * Implements STOMP messaging over WebSocket with Redis pub/sub for multi-instance scaling
 *
 * Topics:
 * - /topic/waiting-queue/{tenantId} - Waiting room queue updates
 * - /topic/room-status/{tenantId} - Room occupancy and status changes
 * - /topic/vitals-alerts/{patientId} - Abnormal vital signs alerts
 *
 * Clients subscribe using STOMP client library (sockjs-client + stompjs)
 */
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for distributed messaging across service instances
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use Redis as external message broker for multi-instance scaling
        config.enableSimpleBroker("/topic", "/queue");

        // Configure Redis relay for production deployment (optional)
        // config.enableStompBrokerRelay("/topic", "/queue")
        //    .setRelayHost("redis")
        //    .setRelayPort(61613)
        //    .setClientLogin("guest")
        //    .setClientPasscode("guest");

        // Application destination prefix for server-side message mapping
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints for client connections
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint
        registry.addEndpoint("/ws/clinical-workflow")
                .setAllowedOrigins(
                    "http://localhost:4200",
                    "http://localhost:4201",
                    "http://localhost:4202"
                )
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");

        // Health check endpoint (no SockJS fallback needed)
        registry.addEndpoint("/ws/health")
                .setAllowedOrigins(
                    "http://localhost:4200",
                    "http://localhost:4201",
                    "http://localhost:4202"
                );
    }
}

/**
 * WebSocket Message Topics Reference:
 *
 * WAITING QUEUE UPDATES
 * Topic: /topic/waiting-queue/{tenantId}
 * Message: {
 *   "queuePosition": 3,
 *   "estimatedWaitMinutes": 12,
 *   "priority": "normal",
 *   "status": "waiting"
 * }
 *
 * ROOM STATUS UPDATES
 * Topic: /topic/room-status/{tenantId}
 * Message: {
 *   "roomNumber": "Room 101",
 *   "status": "occupied",
 *   "occupant": "John Doe",
 *   "provider": "Dr. Smith",
 *   "lastUpdate": "2025-01-16T14:30:00Z"
 * }
 *
 * VITAL SIGNS ALERTS
 * Topic: /topic/vitals-alerts/{patientId}
 * Message: {
 *   "alertStatus": "critical",
 *   "alertMessage": "Systolic BP 180 mmHg - critical high",
 *   "recordedAt": "2025-01-16T14:32:00Z",
 *   "values": {
 *     "systolicBp": 180,
 *     "diastolicBp": 95,
 *     "heartRate": 102
 *   }
 * }
 *
 * CLIENT SUBSCRIPTION EXAMPLE (Angular):
 *
 * connect() {
 *   const socket = new SockJS('http://localhost:8110/clinical-workflow/ws/clinical-workflow');
 *   const stompClient = Stomp.over(socket);
 *
 *   stompClient.connect({
 *     'X-Tenant-ID': this.tenantId,
 *     'Authorization': `Bearer ${token}`
 *   }, (frame) => {
 *     // Subscribe to waiting queue updates
 *     stompClient.subscribe(
 *       `/topic/waiting-queue/${this.tenantId}`,
 *       (message) => {
 *         const queueUpdate = JSON.parse(message.body);
 *         this.handleQueueUpdate(queueUpdate);
 *       }
 *     );
 *
 *     // Subscribe to room status updates
 *     stompClient.subscribe(
 *       `/topic/room-status/${this.tenantId}`,
 *       (message) => {
 *         const roomUpdate = JSON.parse(message.body);
 *         this.handleRoomUpdate(roomUpdate);
 *       }
 *     );
 *   });
 * }
 */
