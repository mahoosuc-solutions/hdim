package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.CodeHierarchyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for HierarchicalCodeSearch.
 */
@DisplayName("HierarchicalCodeSearch TDD Tests")
class HierarchicalCodeSearchTest {

    private HierarchicalCodeSearch search;

    @BeforeEach
    void setUp() {
        search = new HierarchicalCodeSearch();
    }

    @Test
    @DisplayName("Should search ICD-10 hierarchy")
    void testSearchIcd10Hierarchy() {
        String code = "E11.65";
        CodeHierarchyNode hierarchy = search.getIcd10Hierarchy(code);
        assertThat(hierarchy).isNotNull();
        assertThat(hierarchy.getParents()).isNotEmpty();
    }

    @Test
    @DisplayName("Should find parent codes")
    void testFindParentCodes() {
        String code = "E11.65";
        List<String> parents = search.getParentCodes(code);
        assertThat(parents).contains("E11", "E11.6");
    }

    @Test
    @DisplayName("Should find child codes")
    void testFindChildCodes() {
        String code = "E11";
        List<String> children = search.getChildCodes(code);
        assertThat(children).isNotEmpty();
    }

    @Test
    @DisplayName("Should find sibling codes")
    void testFindSiblingCodes() {
        String code = "E11.9";
        List<String> siblings = search.getSiblingCodes(code);
        assertThat(siblings).isNotEmpty();
    }

    @Test
    @DisplayName("Should navigate code hierarchy")
    void testNavigateHierarchy() {
        String code = "E11.65";
        var navigation = search.navigateHierarchy(code);
        assertThat(navigation.getParent()).isNotNull();
        assertThat(navigation.getChildren()).isNotNull();
    }

    @Test
    @DisplayName("Should find root category")
    void testFindRootCategory() {
        String code = "E11.65";
        String root = search.getRootCategory(code);
        assertThat(root).startsWith("E");
    }
}
