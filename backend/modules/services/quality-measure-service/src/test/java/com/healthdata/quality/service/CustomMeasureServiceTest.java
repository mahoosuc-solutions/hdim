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
    void shouldListDrafts() {
        when(repository.findByTenantIdAndStatusOrderByCreatedAtDesc(eq("tenant"), eq("DRAFT")))
                .thenReturn(List.of(CustomMeasureEntity.builder().tenantId("tenant").status("DRAFT").build()));

        List<CustomMeasureEntity> drafts = service.list("tenant", "DRAFT");

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByTenantIdAndId("tenant", id)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.getById("tenant", id));
    }
}
