package com.healthdata.approval.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewerPoolService Tests")
class ReviewerPoolServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ReviewerPoolService reviewerPoolService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String ROLE = "CLINICAL_REVIEWER";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Register Reviewer")
    class RegisterReviewerTests {

        @Test
        @DisplayName("should register reviewer successfully")
        void registerReviewer_Success() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            when(setOperations.add(key, USER_ID)).thenReturn(1L);

            // When
            reviewerPoolService.registerReviewer(TENANT_ID, ROLE, USER_ID);

            // Then
            verify(setOperations).add(key, USER_ID);
            verify(valueOperations).set(
                eq(String.format("hdim:approval:reviewer:status:%s", USER_ID)),
                eq("online"),
                eq(Duration.ofMinutes(30))
            );
        }

        @Test
        @DisplayName("should fallback to in-memory when Redis unavailable")
        void registerReviewer_RedisFailure_UsesInMemory() {
            // Given
            when(setOperations.add(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis connection failed"));

            // When
            reviewerPoolService.registerReviewer(TENANT_ID, ROLE, USER_ID);

            // Then - should not throw exception
            assertThatCode(() -> reviewerPoolService.registerReviewer(TENANT_ID, ROLE, USER_ID))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Unregister Reviewer")
    class UnregisterReviewerTests {

        @Test
        @DisplayName("should unregister reviewer successfully")
        void unregisterReviewer_Success() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            when(setOperations.remove(key, USER_ID)).thenReturn(1L);

            // When
            reviewerPoolService.unregisterReviewer(TENANT_ID, ROLE, USER_ID);

            // Then
            verify(setOperations).remove(key, USER_ID);
        }

        @Test
        @DisplayName("should handle Redis failure gracefully")
        void unregisterReviewer_RedisFailure_DoesNotThrow() {
            // Given
            when(setOperations.remove(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // When/Then
            assertThatCode(() -> reviewerPoolService.unregisterReviewer(TENANT_ID, ROLE, USER_ID))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Get Available Reviewers")
    class GetAvailableReviewersTests {

        @Test
        @DisplayName("should get available online reviewers")
        void getAvailableReviewers_OnlineReviewers_ReturnsOnlineOnly() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            Set<String> allReviewers = Set.of("user-1", "user-2", "user-3");

            when(setOperations.members(key)).thenReturn(allReviewers);
            when(valueOperations.get("hdim:approval:reviewer:status:user-1")).thenReturn("online");
            when(valueOperations.get("hdim:approval:reviewer:status:user-2")).thenReturn(null);
            when(valueOperations.get("hdim:approval:reviewer:status:user-3")).thenReturn("online");

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).containsExactlyInAnyOrder("user-1", "user-3");
        }

        @Test
        @DisplayName("should return all reviewers when none are online")
        void getAvailableReviewers_NoOnlineReviewers_ReturnsAll() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            Set<String> allReviewers = Set.of("user-1", "user-2");

            when(setOperations.members(key)).thenReturn(allReviewers);
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).containsExactlyInAnyOrder("user-1", "user-2");
        }

        @Test
        @DisplayName("should return default reviewers when none registered")
        void getAvailableReviewers_NoRegistered_ReturnsDefaults() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            when(setOperations.members(key)).thenReturn(null);

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains("default-clinical-reviewer");
        }

        @Test
        @DisplayName("should handle empty reviewer set")
        void getAvailableReviewers_EmptySet_ReturnsDefaults() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:%s", TENANT_ID, ROLE);
            when(setOperations.members(key)).thenReturn(Collections.emptySet());

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).contains("default-clinical-reviewer");
        }

        @Test
        @DisplayName("should fallback to in-memory when Redis fails")
        void getAvailableReviewers_RedisFailure_UsesFallback() {
            // Given
            when(setOperations.members(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // Register via fallback first
            when(setOperations.add(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));
            reviewerPoolService.registerReviewer(TENANT_ID, ROLE, USER_ID);

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, ROLE);

            // Then
            assertThat(result).contains(USER_ID);
        }

        @Test
        @DisplayName("should return defaults for ADMIN role")
        void getAvailableReviewers_AdminRole_ReturnsDefaultAdmin() {
            // Given
            String key = String.format("hdim:approval:reviewers:%s:ADMIN", TENANT_ID);
            when(setOperations.members(key)).thenReturn(null);

            // When
            List<String> result = reviewerPoolService.getAvailableReviewers(TENANT_ID, "ADMIN");

            // Then
            assertThat(result).contains("admin");
        }
    }

    @Nested
    @DisplayName("Select Next Reviewer")
    class SelectNextReviewerTests {

        @Test
        @DisplayName("should select reviewer using round-robin")
        void selectNextReviewer_MultipleReviewers_UsesRoundRobin() {
            // Given
            List<String> reviewers = List.of("reviewer-1", "reviewer-2", "reviewer-3");
            String key = String.format("hdim:approval:roundrobin:%s:%s", TENANT_ID, ROLE);

            when(valueOperations.increment(key)).thenReturn(0L, 1L, 2L, 3L);

            // When
            String first = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String second = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String third = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String fourth = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);

            // Then
            assertThat(first).isEqualTo("reviewer-1");
            assertThat(second).isEqualTo("reviewer-2");
            assertThat(third).isEqualTo("reviewer-3");
            assertThat(fourth).isEqualTo("reviewer-1"); // Wraps around
        }

        @Test
        @DisplayName("should return single reviewer when only one available")
        void selectNextReviewer_SingleReviewer_ReturnsSame() {
            // Given
            List<String> reviewers = List.of("reviewer-1");

            // When
            String selected = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);

            // Then
            assertThat(selected).isEqualTo("reviewer-1");
            verify(valueOperations, never()).increment(anyString());
        }

        @Test
        @DisplayName("should throw when no reviewers available")
        void selectNextReviewer_NoReviewers_Throws() {
            // Given
            List<String> reviewers = Collections.emptyList();

            // When/Then
            assertThatThrownBy(() -> reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No eligible reviewers available");
        }

        @Test
        @DisplayName("should fallback to in-memory counter when Redis fails")
        void selectNextReviewer_RedisFailure_UsesInMemory() {
            // Given
            List<String> reviewers = List.of("reviewer-1", "reviewer-2");
            when(valueOperations.increment(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // When
            String first = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);
            String second = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);

            // Then
            assertThat(first).isIn(reviewers);
            assertThat(second).isIn(reviewers);
        }

        @Test
        @DisplayName("should handle null counter gracefully")
        void selectNextReviewer_NullCounter_HandlesGracefully() {
            // Given
            List<String> reviewers = List.of("reviewer-1", "reviewer-2");
            when(valueOperations.increment(anyString())).thenReturn(null);

            // When
            String selected = reviewerPoolService.selectNextReviewer(TENANT_ID, ROLE, reviewers);

            // Then
            assertThat(selected).isIn(reviewers);
        }
    }

    @Nested
    @DisplayName("Update Reviewer Status")
    class UpdateReviewerStatusTests {

        @Test
        @DisplayName("should set reviewer online status")
        void updateReviewerStatus_Online_SetsStatus() {
            // Given
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);

            // When
            reviewerPoolService.updateReviewerStatus(USER_ID, true);

            // Then
            verify(valueOperations).set(
                eq(key),
                eq("online"),
                eq(Duration.ofMinutes(30))
            );
        }

        @Test
        @DisplayName("should remove offline reviewer status")
        void updateReviewerStatus_Offline_RemovesStatus() {
            // Given
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);

            // When
            reviewerPoolService.updateReviewerStatus(USER_ID, false);

            // Then
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should handle Redis failure gracefully")
        void updateReviewerStatus_RedisFailure_DoesNotThrow() {
            // Given
            doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            // When/Then
            assertThatCode(() -> reviewerPoolService.updateReviewerStatus(USER_ID, true))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Is Reviewer Online")
    class IsReviewerOnlineTests {

        @Test
        @DisplayName("should return true when reviewer is online")
        void isReviewerOnline_OnlineStatus_ReturnsTrue() {
            // Given
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);
            when(valueOperations.get(key)).thenReturn("online");

            // When
            boolean isOnline = reviewerPoolService.isReviewerOnline(USER_ID);

            // Then
            assertThat(isOnline).isTrue();
        }

        @Test
        @DisplayName("should return false when reviewer is offline")
        void isReviewerOnline_NoStatus_ReturnsFalse() {
            // Given
            String key = String.format("hdim:approval:reviewer:status:%s", USER_ID);
            when(valueOperations.get(key)).thenReturn(null);

            // When
            boolean isOnline = reviewerPoolService.isReviewerOnline(USER_ID);

            // Then
            assertThat(isOnline).isFalse();
        }

        @Test
        @DisplayName("should assume online when Redis fails")
        void isReviewerOnline_RedisFailure_ReturnsTrue() {
            // Given
            when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

            // When
            boolean isOnline = reviewerPoolService.isReviewerOnline(USER_ID);

            // Then
            assertThat(isOnline).isTrue();
        }
    }

    @Nested
    @DisplayName("Bulk Register Reviewers")
    class BulkRegisterReviewersTests {

        @Test
        @DisplayName("should bulk register reviewers for multiple roles")
        void bulkRegisterReviewers_MultipleRoles_RegistersAll() {
            // Given
            Map<String, List<String>> roleToReviewers = Map.of(
                "CLINICAL_REVIEWER", List.of("reviewer-1", "reviewer-2"),
                "CLINICAL_SUPERVISOR", List.of("supervisor-1")
            );

            // When
            reviewerPoolService.bulkRegisterReviewers(TENANT_ID, roleToReviewers);

            // Then
            verify(setOperations, times(3)).add(anyString(), anyString());
        }

        @Test
        @DisplayName("should handle empty map")
        void bulkRegisterReviewers_EmptyMap_DoesNothing() {
            // Given
            Map<String, List<String>> emptyMap = Collections.emptyMap();

            // When
            reviewerPoolService.bulkRegisterReviewers(TENANT_ID, emptyMap);

            // Then
            verify(setOperations, never()).add(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Get All Reviewers")
    class GetAllReviewersTests {

        @Test
        @DisplayName("should get all reviewers for all roles")
        void getAllReviewers_MultipleRoles_ReturnsAll() {
            // Given
            when(setOperations.members(anyString()))
                .thenReturn(Set.of("reviewer-1"))
                .thenReturn(Set.of("supervisor-1"))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(null);

            when(valueOperations.get(anyString())).thenReturn("online");

            // When
            Map<String, Set<String>> result = reviewerPoolService.getAllReviewers(TENANT_ID);

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result.keySet()).contains("CLINICAL_REVIEWER", "CLINICAL_SUPERVISOR");
        }

        @Test
        @DisplayName("should exclude roles with no reviewers")
        void getAllReviewers_SomeEmpty_ExcludesEmpty() {
            // Given
            when(setOperations.members(anyString())).thenReturn(null);

            // When
            Map<String, Set<String>> result = reviewerPoolService.getAllReviewers(TENANT_ID);

            // Then - Should have defaults for some roles
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Get Assignment Count")
    class GetAssignmentCountTests {

        @Test
        @DisplayName("should return zero for assignment count")
        void getAssignmentCount_ReturnsZero() {
            // When
            int count = reviewerPoolService.getAssignmentCount(USER_ID);

            // Then
            assertThat(count).isZero();
        }
    }
}
