package com.healthdata.investor.controller;

import com.healthdata.investor.dto.TaskDTO;
import com.healthdata.investor.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing investor tasks.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskDTO> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get tasks by status")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable String status) {
        return ResponseEntity.ok(taskService.getTasksByStatus(status));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get tasks by category")
    public ResponseEntity<List<TaskDTO>> getTasksByCategory(@PathVariable String category) {
        return ResponseEntity.ok(taskService.getTasksByCategory(category));
    }

    @GetMapping("/week/{week}")
    @Operation(summary = "Get tasks by week")
    public ResponseEntity<List<TaskDTO>> getTasksByWeek(@PathVariable Integer week) {
        return ResponseEntity.ok(taskService.getTasksByWeek(week));
    }

    @GetMapping("/search")
    @Operation(summary = "Search tasks")
    public ResponseEntity<List<TaskDTO>> searchTasks(@RequestParam String query) {
        return ResponseEntity.ok(taskService.searchTasks(query));
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskDTO.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable UUID id,
            @RequestBody TaskDTO.UpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        TaskDTO.UpdateRequest request = TaskDTO.UpdateRequest.builder().status(status).build();
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
