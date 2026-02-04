package com.healthdata.investor.service;

import com.healthdata.investor.dto.TaskDTO;
import com.healthdata.investor.entity.InvestorTask;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 * Tests CRUD operations and task filtering for investor tasks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task Service Tests")
@Tag("unit")
class TaskServiceTest {

    @Mock
    private InvestorTaskRepository taskRepository;

    @Captor
    private ArgumentCaptor<InvestorTask> taskCaptor;

    private TaskService taskService;

    private static final UUID TEST_TASK_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    private InvestorTask createTestTask(UUID id, String subject, String status) {
        return InvestorTask.builder()
                .id(id)
                .subject(subject)
                .description("Test task description")
                .status(status)
                .category("Legal")
                .week(1)
                .deliverable("Task deliverable")
                .owner("John Doe")
                .dueDate(LocalDate.now().plusDays(7))
                .sortOrder(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Get All Tasks Tests")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return all tasks ordered by week and sort order")
        void shouldReturnAllTasksOrdered() {
            // Given
            List<InvestorTask> tasks = List.of(
                    createTestTask(UUID.randomUUID(), "Task 1", "pending"),
                    createTestTask(UUID.randomUUID(), "Task 2", "completed")
            );
            when(taskRepository.findAllOrderByWeekAndSortOrder()).thenReturn(tasks);

            // When
            List<TaskDTO> result = taskService.getAllTasks();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSubject()).isEqualTo("Task 1");
            assertThat(result.get(1).getSubject()).isEqualTo("Task 2");
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyListWhenNoTasks() {
            // Given
            when(taskRepository.findAllOrderByWeekAndSortOrder()).thenReturn(List.of());

            // When
            List<TaskDTO> result = taskService.getAllTasks();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Task By ID Tests")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return task when found")
        void shouldReturnTaskWhenFound() {
            // Given
            InvestorTask task = createTestTask(TEST_TASK_ID, "Test Task", "pending");
            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(task));

            // When
            TaskDTO result = taskService.getTask(TEST_TASK_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TEST_TASK_ID);
            assertThat(result.getSubject()).isEqualTo("Test Task");
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowWhenTaskNotFound() {
            // Given
            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskService.getTask(TEST_TASK_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task");
        }
    }

    @Nested
    @DisplayName("Filter Tasks Tests")
    class FilterTasksTests {

        @Test
        @DisplayName("Should filter tasks by status")
        void shouldFilterByStatus() {
            // Given
            List<InvestorTask> pendingTasks = List.of(
                    createTestTask(UUID.randomUUID(), "Pending Task", "pending")
            );
            when(taskRepository.findByStatusOrderBySortOrderAsc("pending")).thenReturn(pendingTasks);

            // When
            List<TaskDTO> result = taskService.getTasksByStatus("pending");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("Should filter tasks by category")
        void shouldFilterByCategory() {
            // Given
            List<InvestorTask> legalTasks = List.of(
                    createTestTask(UUID.randomUUID(), "Legal Task", "pending")
            );
            when(taskRepository.findByCategoryOrderBySortOrderAsc("Legal")).thenReturn(legalTasks);

            // When
            List<TaskDTO> result = taskService.getTasksByCategory("Legal");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("Legal");
        }

        @Test
        @DisplayName("Should filter tasks by week")
        void shouldFilterByWeek() {
            // Given
            List<InvestorTask> weekOneTasks = List.of(
                    createTestTask(UUID.randomUUID(), "Week 1 Task", "pending")
            );
            when(taskRepository.findByWeekOrderBySortOrderAsc(1)).thenReturn(weekOneTasks);

            // When
            List<TaskDTO> result = taskService.getTasksByWeek(1);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWeek()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should search tasks by query")
        void shouldSearchTasks() {
            // Given
            List<InvestorTask> matchingTasks = List.of(
                    createTestTask(UUID.randomUUID(), "Investor Meeting", "pending")
            );
            when(taskRepository.findBySubjectContainingIgnoreCaseOrDescriptionContainingIgnoreCase("meeting", "meeting"))
                    .thenReturn(matchingTasks);

            // When
            List<TaskDTO> result = taskService.searchTasks("meeting");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSubject()).contains("Meeting");
        }
    }

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task with auto-assigned sort order")
        void shouldCreateTaskWithSortOrder() {
            // Given
            when(taskRepository.count()).thenReturn(5L);
            when(taskRepository.save(any(InvestorTask.class))).thenAnswer(inv -> {
                InvestorTask task = inv.getArgument(0);
                task.setId(TEST_TASK_ID);
                return task;
            });

            TaskDTO.CreateRequest request = TaskDTO.CreateRequest.builder()
                    .subject("New Task")
                    .description("Task description")
                    .category("Financial")
                    .week(2)
                    .deliverable("Deliverable")
                    .owner("Jane Doe")
                    .dueDate(LocalDate.now().plusDays(14))
                    .build();

            // When
            TaskDTO result = taskService.createTask(request);

            // Then
            verify(taskRepository).save(taskCaptor.capture());
            InvestorTask savedTask = taskCaptor.getValue();
            assertThat(savedTask.getSortOrder()).isEqualTo(6);
            assertThat(savedTask.getStatus()).isEqualTo("pending");
            assertThat(result.getSubject()).isEqualTo("New Task");
        }

        @Test
        @DisplayName("Should set default status to pending")
        void shouldSetDefaultStatusToPending() {
            // Given
            when(taskRepository.count()).thenReturn(0L);
            when(taskRepository.save(any(InvestorTask.class))).thenAnswer(inv -> {
                InvestorTask task = inv.getArgument(0);
                task.setId(TEST_TASK_ID);
                return task;
            });

            TaskDTO.CreateRequest request = TaskDTO.CreateRequest.builder()
                    .subject("Task")
                    .build();

            // When
            taskService.createTask(request);

            // Then
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getStatus()).isEqualTo("pending");
        }
    }

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task fields selectively")
        void shouldUpdateTaskFieldsSelectively() {
            // Given
            InvestorTask existingTask = createTestTask(TEST_TASK_ID, "Original Subject", "pending");
            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.save(any(InvestorTask.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.UpdateRequest request = TaskDTO.UpdateRequest.builder()
                    .subject("Updated Subject")
                    .build();

            // When
            TaskDTO result = taskService.updateTask(TEST_TASK_ID, request);

            // Then
            assertThat(result.getSubject()).isEqualTo("Updated Subject");
            assertThat(result.getDescription()).isEqualTo("Test task description"); // Not changed
        }

        @Test
        @DisplayName("Should set completedAt when status changes to completed")
        void shouldSetCompletedAtOnCompletion() {
            // Given
            InvestorTask existingTask = createTestTask(TEST_TASK_ID, "Task", "pending");
            existingTask.setCompletedAt(null);
            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.save(any(InvestorTask.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.UpdateRequest request = TaskDTO.UpdateRequest.builder()
                    .status("completed")
                    .build();

            // When
            TaskDTO result = taskService.updateTask(TEST_TASK_ID, request);

            // Then
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getCompletedAt()).isNotNull();
            assertThat(result.getStatus()).isEqualTo("completed");
        }

        @Test
        @DisplayName("Should not overwrite completedAt if already set")
        void shouldNotOverwriteCompletedAt() {
            // Given
            Instant originalCompletedAt = Instant.now().minusSeconds(86400); // 1 day ago
            InvestorTask existingTask = createTestTask(TEST_TASK_ID, "Task", "completed");
            existingTask.setCompletedAt(originalCompletedAt);

            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.of(existingTask));
            when(taskRepository.save(any(InvestorTask.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskDTO.UpdateRequest request = TaskDTO.UpdateRequest.builder()
                    .notes("Additional notes")
                    .build();

            // When
            taskService.updateTask(TEST_TASK_ID, request);

            // Then
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getCompletedAt()).isEqualTo(originalCompletedAt);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent task")
        void shouldThrowWhenUpdatingNonExistentTask() {
            // Given
            when(taskRepository.findById(TEST_TASK_ID)).thenReturn(Optional.empty());

            TaskDTO.UpdateRequest request = TaskDTO.UpdateRequest.builder()
                    .subject("Updated")
                    .build();

            // When/Then
            assertThatThrownBy(() -> taskService.updateTask(TEST_TASK_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task when exists")
        void shouldDeleteTaskWhenExists() {
            // Given
            when(taskRepository.existsById(TEST_TASK_ID)).thenReturn(true);

            // When
            taskService.deleteTask(TEST_TASK_ID);

            // Then
            verify(taskRepository).deleteById(TEST_TASK_ID);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent task")
        void shouldThrowWhenDeletingNonExistentTask() {
            // Given
            when(taskRepository.existsById(TEST_TASK_ID)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> taskService.deleteTask(TEST_TASK_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepository, never()).deleteById(any());
        }
    }
}
