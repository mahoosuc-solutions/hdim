package com.healthdata.investor.service;

import com.healthdata.investor.dto.TaskDTO;
import com.healthdata.investor.entity.InvestorTask;
import com.healthdata.investor.exception.ResourceNotFoundException;
import com.healthdata.investor.repository.InvestorTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing investor tasks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final InvestorTaskRepository taskRepository;

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAllOrderByWeekAndSortOrder()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO getTask(UUID id) {
        return taskRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));
    }

    public List<TaskDTO> getTasksByStatus(String status) {
        return taskRepository.findByStatusOrderBySortOrderAsc(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByCategory(String category) {
        return taskRepository.findByCategoryOrderBySortOrderAsc(category)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByWeek(Integer week) {
        return taskRepository.findByWeekOrderBySortOrderAsc(week)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> searchTasks(String query) {
        return taskRepository.findBySubjectContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO.CreateRequest request) {
        long maxSortOrder = taskRepository.count();

        InvestorTask task = InvestorTask.builder()
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(request.getCategory())
                .week(request.getWeek())
                .deliverable(request.getDeliverable())
                .owner(request.getOwner())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .sortOrder((int) (maxSortOrder + 1))
                .status("pending")
                .build();

        task = taskRepository.save(task);
        log.info("Created task: {} with ID: {}", task.getSubject(), task.getId());
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(UUID id, TaskDTO.UpdateRequest request) {
        InvestorTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id.toString()));

        if (request.getSubject() != null) task.setSubject(request.getSubject());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            if ("completed".equals(request.getStatus()) && task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            }
        }
        if (request.getCategory() != null) task.setCategory(request.getCategory());
        if (request.getWeek() != null) task.setWeek(request.getWeek());
        if (request.getDeliverable() != null) task.setDeliverable(request.getDeliverable());
        if (request.getOwner() != null) task.setOwner(request.getOwner());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getNotes() != null) task.setNotes(request.getNotes());

        task = taskRepository.save(task);
        log.info("Updated task: {}", task.getId());
        return toDTO(task);
    }

    @Transactional
    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id.toString());
        }
        taskRepository.deleteById(id);
        log.info("Deleted task: {}", id);
    }

    private TaskDTO toDTO(InvestorTask task) {
        return TaskDTO.builder()
                .id(task.getId())
                .subject(task.getSubject())
                .description(task.getDescription())
                .status(task.getStatus())
                .category(task.getCategory())
                .week(task.getWeek())
                .deliverable(task.getDeliverable())
                .owner(task.getOwner())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .notes(task.getNotes())
                .sortOrder(task.getSortOrder())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
