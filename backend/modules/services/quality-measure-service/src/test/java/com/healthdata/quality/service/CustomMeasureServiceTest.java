package com.healthdata.quality.service;

import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CustomMeasureServiceTest {

    @Mock
    private CustomMeasureRepository repository;

    @InjectMocks
    private CustomMeasureService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateDraft() {
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> {
            CustomMeasureEntity e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CustomMeasureEntity saved = service.createDraft("tenant", "NAME", "desc", "CUSTOM", 2024, "tester");

        assertThat(saved.getTenantId()).isEqualTo("tenant");
        assertThat(saved.getName()).isEqualTo("NAME");
        verify(repository, times(1)).save(any(CustomMeasureEntity.class));
    }

    @Test
    void shouldUpdateDraft() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity existing = CustomMeasureEntity.builder()
                .id(id)
                .tenantId("tenant")
                .name("OLD")
                .version("1.0.0")
                .status("DRAFT")
                .createdBy("tester")
                .build();
        when(repository.findByTenantIdAndId("tenant", id)).thenReturn(Optional.of(existing));
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomMeasureEntity updated = service.updateDraft("tenant", id, "NEW", "desc", "CUSTOM", 2025);

        assertThat(updated.getName()).isEqualTo("NEW");
        assertThat(updated.getCategory()).isEqualTo("CUSTOM");
        assertThat(updated.getYear()).isEqualTo(2025);
        verify(repository, times(1)).save(any(CustomMeasureEntity.class));
    }

    @Test
    void shouldNotOverwriteNameWhenBlank() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity existing = CustomMeasureEntity.builder()
                .id(id)
                .tenantId("tenant")
                .name("EXISTING")
                .status("DRAFT")
                .createdBy("tester")
                .build();
        when(repository.findByTenantIdAndId("tenant", id)).thenReturn(Optional.of(existing));
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomMeasureEntity updated = service.updateDraft("tenant", id, " ", "new", "NEWCAT", 2025);

        assertThat(updated.getName()).isEqualTo("EXISTING");
        assertThat(updated.getDescription()).isEqualTo("new");
        assertThat(updated.getCategory()).isEqualTo("NEWCAT");
        assertThat(updated.getYear()).isEqualTo(2025);
    }

    @Test
    void shouldListDrafts() {
        when(repository.findByTenantIdAndStatusOrderByCreatedAtDesc(eq("tenant"), eq("DRAFT")))
                .thenReturn(List.of(CustomMeasureEntity.builder().tenantId("tenant").status("DRAFT").build()));

        List<CustomMeasureEntity> drafts = service.list("tenant", "DRAFT");

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void shouldListAllWhenStatusMissing() {
        when(repository.findByTenantIdOrderByCreatedAtDesc("tenant"))
                .thenReturn(List.of(CustomMeasureEntity.builder().tenantId("tenant").status("PUBLISHED").build()));

        List<CustomMeasureEntity> measures = service.list("tenant", null);

        assertThat(measures).hasSize(1);
        assertThat(measures.get(0).getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByTenantIdAndId("tenant", id)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.getById("tenant", id));
    }

    @Test
    void batchPublishShouldPublishDraftsAndSkipOthers() {
        UUID draftId = UUID.randomUUID();
        UUID publishedId = UUID.randomUUID();
        CustomMeasureEntity draft = CustomMeasureEntity.builder()
                .id(draftId)
                .tenantId("tenant")
                .status("DRAFT")
                .build();
        CustomMeasureEntity published = CustomMeasureEntity.builder()
                .id(publishedId)
                .tenantId("tenant")
                .status("PUBLISHED")
                .build();

        when(repository.findByTenantIdAndIdIn("tenant", List.of(draftId, publishedId)))
                .thenReturn(List.of(draft, published));
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomMeasureService.BatchPublishResult result = service.batchPublish(
                "tenant",
                List.of(draftId, publishedId),
                "publisher"
        );

        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(draft.getStatus()).isEqualTo("PUBLISHED");
        assertThat(draft.getPublishedDate()).isNotNull();
        verify(repository, times(1)).save(eq(draft));
    }

    @Test
    void batchPublishShouldThrowWhenIdsMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByTenantIdAndIdIn("tenant", List.of(id))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.batchPublish(
                "tenant",
                List.of(id),
                "publisher"
        ));
    }

    @Test
    void batchDeleteShouldFailWhenEvaluationsExistAndNoForce() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity measure = CustomMeasureEntity.builder()
                .id(id)
                .tenantId("tenant")
                .status("DRAFT")
                .build();

        when(repository.findByTenantIdAndIdIn("tenant", List.of(id))).thenReturn(List.of(measure));
        when(repository.countEvaluationsByMeasureIds(List.of(id))).thenReturn(2L);

        CustomMeasureService.BatchDeleteResult result = service.batchDelete(
                "tenant",
                List.of(id),
                "deleter",
                false
        );

        assertThat(result.deletedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.measuresInUse()).contains(id.toString());
        verify(repository, never()).save(any(CustomMeasureEntity.class));
    }

    @Test
    void batchDeleteShouldForceDeleteWhenRequested() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity measure = CustomMeasureEntity.builder()
                .id(id)
                .tenantId("tenant")
                .status("DRAFT")
                .build();

        when(repository.findByTenantIdAndIdIn("tenant", List.of(id))).thenReturn(List.of(measure));
        when(repository.countEvaluationsByMeasureIds(List.of(id))).thenReturn(3L);
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomMeasureService.BatchDeleteResult result = service.batchDelete(
                "tenant",
                List.of(id),
                "deleter",
                true
        );

        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(0);
        assertThat(measure.getDeletedAt()).isNotNull();
        assertThat(measure.getDeletedBy()).isEqualTo("deleter");
    }

    @Test
    void shouldSoftDeleteSingleMeasure() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity measure = CustomMeasureEntity.builder()
                .id(id)
                .tenantId("tenant")
                .status("DRAFT")
                .build();
        when(repository.findByTenantIdAndId("tenant", id)).thenReturn(Optional.of(measure));
        when(repository.save(any(CustomMeasureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete("tenant", id, "deleter");

        assertThat(measure.getDeletedAt()).isNotNull();
        assertThat(measure.getDeletedBy()).isEqualTo("deleter");
    }
}
