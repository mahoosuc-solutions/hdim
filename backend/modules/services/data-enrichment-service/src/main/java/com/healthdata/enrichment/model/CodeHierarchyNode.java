package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Node in code hierarchy tree.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeHierarchyNode {
    private String code;
    private String description;
    private int level;

    @Builder.Default
    private List<String> parents = new ArrayList<>();

    @Builder.Default
    private List<String> children = new ArrayList<>();
}
