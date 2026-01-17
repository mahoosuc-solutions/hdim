package com.healthdata.fhir.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.fhir.persistence.TaskEntity;
import com.healthdata.fhir.persistence.TaskRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing FHIR Task resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#tenantId + ':' + #result.idElement.idPart"),
            @CacheEvict(value = "tasksByPatient", allEntries = true)
    })
    public Task createTask(String tenantId, Task task, String createdBy) {
        validateTask(task);
        UUID taskId = ensureTaskId(task);
        UUID patientId = extractPatientId(task);

        TaskEntity entity = toEntity(tenantId, taskId, patientId, task, createdBy, createdBy);
        TaskEntity saved = taskRepository.save(entity);

        Task savedTask = toFhirResource(saved);
        publishTaskEvent("fhir.tasks.created", tenantId, taskId.toString(), savedTask, createdBy);
        return savedTask;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#tenantId + ':' + #id")
    public Optional<Task> getTask(String tenantId, String id) {
        try {
            UUID taskId = UUID.fromString(id);
            return taskRepository.findByTenantIdAndId(tenantId, taskId)
                    .map(this::toFhirResource);
        } catch (IllegalArgumentException e) {
            log.error("Invalid task ID format: {}", id);
            return Optional.empty();
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "tasksByPatient", allEntries = true)
    })
    public Task updateTask(String tenantId, String id, Task task, String modifiedBy) {
        UUID taskId = UUID.fromString(id);
        TaskEntity existing = taskRepository.findByTenantIdAndId(tenantId, taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));

        validateTask(task);
        task.setId(id);
        UUID patientId = extractPatientId(task);

        existing.setResourceJson(JSON_PARSER.encodeResourceToString(task));
        existing.setPatientId(patientId);
        updateExtractedFields(existing, task);

        TaskEntity updated = taskRepository.save(existing);
        Task updatedTask = toFhirResource(updated);

        publishTaskEvent("fhir.tasks.updated", tenantId, id, updatedTask, modifiedBy);
        return updatedTask;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "tasksByPatient", allEntries = true)
    })
    public void deleteTask(String tenantId, String id, String deletedBy) {
        UUID taskId = UUID.fromString(id);
        TaskEntity task = taskRepository.findByTenantIdAndId(tenantId, taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));

        taskRepository.delete(task);
        publishTaskEvent("fhir.tasks.deleted", tenantId, id, null, deletedBy);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tasksByPatient", key = "#tenantId + ':' + #patientId")
    public Bundle searchTasksByPatient(String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = UUID.fromString(patientId);
        Page<TaskEntity> page = taskRepository.findByTenantIdAndPatientIdOrderByAuthoredOnDesc(
                tenantId, patientUuid, pageable);
        return createBundle(page.getContent(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Bundle searchTasksByPatientAndDateRange(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        List<TaskEntity> tasks = taskRepository.findByPatientAndDateRange(
                tenantId, patientUuid, startDate, endDate);
        return createBundle(tasks, tasks.size());
    }

    @Transactional(readOnly = true)
    public Bundle searchTasksByDateRange(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        List<TaskEntity> tasks = taskRepository.findByTenantAndDateRange(
                tenantId, startDate, endDate);
        return createBundle(tasks, tasks.size());
    }

    private void validateTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getFor() == null || task.getFor().getReference() == null) {
            throw new IllegalArgumentException("Task must have a patient reference");
        }
    }

    private UUID ensureTaskId(Task task) {
        if (task.hasId()) {
            return UUID.fromString(task.getIdElement().getIdPart());
        }
        UUID newId = UUID.randomUUID();
        task.setId(newId.toString());
        return newId;
    }

    private UUID extractPatientId(Task task) {
        String patientRef = task.getFor().getReference();
        String patientId = patientRef.contains("/") ? patientRef.substring(patientRef.lastIndexOf("/") + 1)
                : patientRef;
        return UUID.fromString(patientId);
    }

    private TaskEntity toEntity(String tenantId, UUID taskId, UUID patientId,
            Task task, String createdBy, String modifiedBy) {
        TaskEntity entity = TaskEntity.builder()
                .id(taskId)
                .tenantId(tenantId)
                .resourceType("Task")
                .resourceJson(JSON_PARSER.encodeResourceToString(task))
                .patientId(patientId)
                .createdBy(createdBy)
                .lastModifiedBy(modifiedBy)
                .build();
        updateExtractedFields(entity, task);
        return entity;
    }

    private void updateExtractedFields(TaskEntity entity, Task task) {
        if (task.hasStatus()) {
            entity.setStatus(task.getStatus().toCode());
        }
        if (task.hasPriority()) {
            entity.setPriority(task.getPriority().toCode());
        }
        if (task.hasCode() && task.getCode().hasCoding()) {
            Coding coding = task.getCode().getCodingFirstRep();
            entity.setTaskCode(coding.getCode());
            entity.setTaskDisplay(coding.getDisplay());
        }
        entity.setAuthoredOn(dateToLocalDateTime(task.getAuthoredOn()));
        if (task.hasExecutionPeriod()) {
            entity.setExecutionStart(dateToLocalDateTime(task.getExecutionPeriod().getStart()));
            entity.setExecutionEnd(dateToLocalDateTime(task.getExecutionPeriod().getEnd()));
        }
        entity.setOwnerId(extractReferenceId(task.getOwner()));
    }

    private String extractReferenceId(Reference reference) {
        if (reference == null || reference.getReference() == null) {
            return null;
        }
        String ref = reference.getReference();
        return ref.contains("/") ? ref.substring(ref.lastIndexOf("/") + 1) : ref;
    }

    private Task toFhirResource(TaskEntity entity) {
        return (Task) JSON_PARSER.parseResource(entity.getResourceJson());
    }

    private Bundle createBundle(List<TaskEntity> tasks, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        List<Bundle.BundleEntryComponent> entries = tasks.stream()
                .map(entity -> {
                    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                    entry.setResource(toFhirResource(entity));
                    entry.setFullUrl("Task/" + entity.getId());
                    return entry;
                })
                .collect(Collectors.toList());

        bundle.setEntry(entries);
        return bundle;
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void publishTaskEvent(
            String topic,
            String tenantId,
            String taskId,
            Task task,
            String actor) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", topic);
            event.put("resourceType", "Task");
            event.put("resourceId", taskId);
            event.put("tenantId", tenantId);
            if (task != null) {
                event.put("patientId", extractPatientId(task).toString());
            }
            event.put("occurredAt", java.time.Instant.now().toString());
            event.put("actor", actor);
            if (task != null) {
                String json = JSON_PARSER.encodeResourceToString(task);
                try {
                    event.put("resource", objectMapper.readValue(json, java.util.Map.class));
                } catch (Exception e) {
                    event.put("resource", json);
                }
            }
            kafkaTemplate.send(topic, tenantId + ":" + taskId, event);
        } catch (Exception e) {
            log.error("Failed to publish task event", e);
        }
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }
}
